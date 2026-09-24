package cn.org.alan.exam.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.converter.UserConverter;
import cn.org.alan.exam.mapper.GradeMapper;
import cn.org.alan.exam.mapper.UserGradeMapper;
import cn.org.alan.exam.mapper.UserMapper;
import cn.org.alan.exam.model.entity.Grade;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.entity.UserGrade;
import cn.org.alan.exam.model.form.user.UserForm;
import cn.org.alan.exam.model.vo.user.UserVO;
import cn.org.alan.exam.service.IFileService;
import cn.org.alan.exam.service.IUserService;
import cn.org.alan.exam.utils.DateTimeUtil;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.excel.ExcelUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * 用户服务实现类
 *
 * @author WeiJin
 * @since 2024-03-21
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private HttpServletRequest request;
    @Resource
    private UserConverter userConverter;
    @Resource
    private GradeMapper gradeMapper;
    @Resource
    private UserGradeMapper userGradeMapper;
    @Resource
    private IFileService fileService;


    /**
     * 创建用户，教师只能创建学生，管理员可以创建教师和学生
     *
     * @param userForm
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> createUser(UserForm userForm) {
        // 设置默认密码
        userForm.setPassword(new BCryptPasswordEncoder().encode("123456"));
        // 获取角色
        Integer roleCode = SecurityUtil.getRoleCode();
        // 教师只能创建学生
        if (roleCode == 2) {
            userForm.setRoleId(1);
        }
        List<Integer> gradeIds = parseGradeIds(userForm);
        if (userForm.getRoleId() != null && userForm.getRoleId() == 2 && !gradeIds.isEmpty()) {
            throw new ServiceRuntimeException("教师无法设置单一班级");
        }
        // 避免管理员创建用户不传递角色
        if (userForm.getRoleId() == null || userForm.getRoleId() == 0) {
            throw new ServiceRuntimeException("未选择用户角色");
        }
        if (userForm.getRoleId() == 1) {
            validateAndCheckTeacherGrades(gradeIds, roleCode);
            userForm.setGradeId(gradeIds.isEmpty() ? null : gradeIds.get(0));
        }
        User user = userConverter.fromToEntity(userForm);
        // 调用Mapper插入用户
        userMapper.insert(user);
        if (userForm.getRoleId() == 1 && !gradeIds.isEmpty()) {
            syncStudentGrades(user.getId(), gradeIds);
        }
        return Result.success("用户创建成功");

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateUser(Integer id, UserForm userForm) {
        User existUser = userMapper.selectById(id);
        if (existUser == null) {
            throw new ServiceRuntimeException("用户不存在");
        }
        if (existUser.getRoleId() != null && existUser.getRoleId() == 3) {
            throw new ServiceRuntimeException("无法修改管理员用户");
        }

        Integer callerRole = SecurityUtil.getRoleCode();
        Integer existRoleId = existUser.getRoleId();
        // 角色不允许修改：学生和教师的班级、考试数据模型不同
        if (callerRole == 2 && (existRoleId == null || existRoleId != 1)) {
            throw new ServiceRuntimeException("教师只能修改学生信息");
        }

        Integer gradeId = null;
        if (existRoleId != null && existRoleId == 1) {
            List<Integer> gradeIds = parseGradeIds(userForm);
            validateAndCheckTeacherGrades(gradeIds, callerRole);
            syncStudentGrades(id, gradeIds);
            gradeId = gradeIds.isEmpty() ? null : gradeIds.get(0);
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getRealName, userForm.getRealName())
                .set(User::getGradeId, gradeId);
        // 密码为空则不重置；有值则重置为新密码
        if (StringUtils.isNotBlank(userForm.getPassword())) {
            String rawPassword = userForm.getPassword().trim();
            if (rawPassword.length() < 6) {
                throw new ServiceRuntimeException("新密码不能少于6位");
            }
            updateWrapper.set(User::getPassword, new BCryptPasswordEncoder().encode(rawPassword));
        }
        int rows = userMapper.update(null, updateWrapper);
        if (rows < 1) {
            throw new ServiceRuntimeException("修改用户失败");
        }
        return Result.success("用户修改成功");
    }

    private List<Integer> parseGradeIds(UserForm userForm) {
        List<Integer> gradeIds = new ArrayList<>();
        if (StringUtils.isNotBlank(userForm.getGradeIds())) {
            gradeIds = Arrays.stream(userForm.getGradeIds().split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(Integer::valueOf)
                    .distinct()
                    .collect(Collectors.toList());
        } else if (userForm.getGradeId() != null) {
            gradeIds.add(userForm.getGradeId());
        }
        return gradeIds;
    }

    private void validateAndCheckTeacherGrades(List<Integer> gradeIds, Integer callerRole) {
        if (gradeIds == null || gradeIds.isEmpty()) {
            return;
        }
        for (Integer gradeId : gradeIds) {
            Grade grade = gradeMapper.selectById(gradeId);
            if (grade == null) {
                throw new ServiceRuntimeException("班级不存在");
            }
        }
        if (callerRole != null && callerRole == 2) {
            List<Integer> teacherGrades = userGradeMapper.getGradeIdListByUserId(SecurityUtil.getUserId());
            if (teacherGrades == null || teacherGrades.isEmpty()) {
                throw new ServiceRuntimeException("教师还未加入任何班级");
            }
            for (Integer gradeId : gradeIds) {
                if (!teacherGrades.contains(gradeId)) {
                    throw new ServiceRuntimeException("只能将学生加入自己所在的班级");
                }
            }
        }
    }

    private void syncStudentGrades(Integer userId, List<Integer> gradeIds) {
        userGradeMapper.deleteByUserId(userId);
        if (gradeIds == null || gradeIds.isEmpty()) {
            return;
        }
        for (Integer gradeId : gradeIds) {
            UserGrade userGrade = new UserGrade();
            userGrade.setUId(userId);
            userGrade.setGId(gradeId);
            userGradeMapper.insert(userGrade);
        }
    }

    private void fillStudentGrades(UserVO userVo) {
        if (userVo == null || userVo.getRoleId() == null || userVo.getRoleId() != 1) {
            return;
        }
        List<Integer> gradeIds = userGradeMapper.getStudentGradeIdList(userVo.getId());
        List<cn.org.alan.exam.model.vo.grade.GradeVO> grades = new ArrayList<>();
        if (gradeIds != null) {
            for (Integer gid : gradeIds) {
                Grade grade = gradeMapper.selectById(gid);
                if (grade == null) {
                    continue;
                }
                cn.org.alan.exam.model.vo.grade.GradeVO gvo = new cn.org.alan.exam.model.vo.grade.GradeVO();
                gvo.setId(grade.getId());
                gvo.setGradeName(grade.getGradeName());
                gvo.setCode(grade.getCode());
                grades.add(gvo);
            }
        }
        userVo.setGrades(grades);
        if (!grades.isEmpty()) {
            userVo.setGradeName(grades.stream()
                    .map(cn.org.alan.exam.model.vo.grade.GradeVO::getGradeName)
                    .collect(Collectors.joining("、")));
            userVo.setGradeId(grades.get(0).getId());
        }
    }

    @Override
    public Result<String> updatePassword(UserForm userForm) {
        Integer userId = SecurityUtil.getUserId();
        if (!userForm.getNewPassword().equals(userForm.getCheckedPassword())) {
            throw new ServiceRuntimeException("两次密码不一致");
        }
        if (!new BCryptPasswordEncoder()
                .matches(userForm.getOriginPassword(), userMapper.selectById(userId).getPassword())) {
            throw new ServiceRuntimeException("旧密码错误");
        }
        // 设置新加密后到密码配置好自己到userID
        userForm.setPassword(new BCryptPasswordEncoder().encode(userForm.getNewPassword()));
        userForm.setId(userId);
        // 转还为User实体
        User user = userConverter.fromToEntity(userForm);
        // 调用mapper更新用户密码
        int updated = userMapper.updateById(user);
        // 密码修改成功清除redis的token，让用户重新登录
        if (updated > 0) {
            stringRedisTemplate.delete("token:" + request.getSession().getId());
            return Result.success("修改成功，请重新登录");
        }
        throw new ServiceRuntimeException("旧密码错误");

    }

    @Override
    @Transactional
    public Result<String> deleteBatchByIds(String ids) {
        List<Integer> userIds = Arrays.stream(ids.split(",")).map(Integer::parseInt).collect(java.util.stream.Collectors.toList());
        List<Integer> adminList = userMapper.getAdminList();
        // 判断删除用户列表集合是否包含管理员列表中的id
        boolean containsAdminId = userIds.stream().anyMatch(adminList::contains);
        if(containsAdminId){
            throw new ServiceRuntimeException("无法删除管理员用户");
        }
        if (userIds.isEmpty()) {
            throw new ServiceRuntimeException("删除数据库时未传入用户Id");
        }
        Integer row = userMapper.deleteBatchIds(userIds);
        if (row < 1) {
            throw new ServiceRuntimeException("删除数据库时失败，条数<1");
        }
        return Result.success("删除成功");
    }

    @SneakyThrows(Exception.class)
    @Override
    @Transactional
    public Result<String> importUsers(MultipartFile file) {
        // 文件类型判断
        if (!ExcelUtils.isExcel(Objects.requireNonNull(file.getOriginalFilename()))) {
            throw new ServiceRuntimeException("文件类型必须是xls或xlsx");
        }
        // 读取文件
        List<UserForm> list = ExcelUtils.readMultipartFile(file, UserForm.class);
        // 参数补充
        list.forEach(userForm -> {
            // 设置默认密码
            userForm.setPassword(new BCryptPasswordEncoder().encode("123456"));
            userForm.setCreateTime(DateTimeUtil.getDateTime());
            if (userForm.getRoleId() == null) {
                // 没有设置角色默认为学生
                userForm.setRoleId(1);
            }
        });
        // 判断条数是否过多
        if (list.size() > 300) {
            throw new ServiceRuntimeException("表中最多存放300条数据");
        }
        userMapper.insertBatchUser(userConverter.listFromToEntity(list));
        return Result.success("用户导入成功");
    }

    /**
     * 获取用户个人信息
     *
     * @return
     */
    @Override
    public Result<UserVO> info() {
        Integer userId = SecurityUtil.getUserId();
        UserVO userVo = userMapper.info(userId);
        userVo.setPassword(null);
        fillStudentGrades(userVo);
        return Result.success("获取用户信息成功", userVo);
    }

    /**
     * 用户加入班级，只有学生才能加入班级
     *
     * @param code
     * @return
     */
    @Override
    public Result<String> joinGrade(String code) {
        Integer userId = SecurityUtil.getUserId();
        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<Grade>().eq(Grade::getCode, code);
        Grade grade = gradeMapper.selectOne(wrapper);
        if (Objects.isNull(grade)) {
            throw new ServiceRuntimeException("班级口令不存在");
        }
        List<Integer> existed = userGradeMapper.getStudentGradeIdList(userId);
        if (existed != null && existed.contains(grade.getId())) {
            return Result.success("已在班级：" + grade.getGradeName() + " 中");
        }
        UserGrade userGrade = new UserGrade();
        userGrade.setUId(userId);
        userGrade.setGId(grade.getId());
        int insert = userGradeMapper.insert(userGrade);
        if (insert < 1) {
            throw new ServiceRuntimeException("加入班级失败,写入关联表失败");
        }
        // 兼容旧逻辑：主班字段更新为最近加入的班级
        User user = new User();
        user.setId(userId);
        user.setGradeId(grade.getId());
        userMapper.updateById(user);
        return Result.success("加入班级：" + grade.getGradeName() + "成功");
    }

    @Override
    public Result<IPage<UserVO>> pagingUser(Integer pageNum, Integer pageSize, Integer gradeId, String realName) {
        IPage<UserVO> page = new Page<>(pageNum, pageSize);
        Integer userId = SecurityUtil.getUserId();
        Integer roleCode = SecurityUtil.getRoleCode();
        if (roleCode == 2) {
            // 如果是教师，要先查询教师加入了那些班级，根据教师到所有班级，查询所有班级下的用户
            List<Integer> gradeIdList = userGradeMapper.getGradeIdListByUserId(userId);
            if(gradeIdList.isEmpty()){
                throw new ServiceRuntimeException("教师还没加入班级暂无数据");
            }
            page = userMapper.pagingUser(page, gradeId, realName, userId, 1, gradeIdList);
        } else {
            // 管理员直接查询所有用户
            page = userMapper.pagingUser(page, gradeId, realName, userId, null, null);
        }
        if (page.getRecords() != null) {
            for (UserVO vo : page.getRecords()) {
                fillStudentGrades(vo);
            }
        }
        return Result.success("分页获取用户信息成功", page);
    }

    @Transactional
    @Override
    public Result<String> uploadAvatar(MultipartFile file) {
        // 1.上传图片
        Integer userId = SecurityUtil.getUserId();
        Result<String> result = fileService.uploadImage(file);
        if (result.getCode() == 0) {
            throw new ServiceRuntimeException("图片上传失败,上传图片代码code为0");
        }
        // 2.设置数据库头像地址
        String url = result.getData();
        User user = new User();
        user.setId(userId);
        user.setAvatar(url);
        Integer row = userMapper.updateById(user);
        if (row > 0) {
            return Result.success("上传成功", url);
        }
        throw new ServiceRuntimeException("图片上传失败,修改用户表头像地址条数<=0");
    }


}
