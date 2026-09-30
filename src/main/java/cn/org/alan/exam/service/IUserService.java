package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.form.user.UserForm;
import cn.org.alan.exam.model.vo.user.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface IUserService extends IService<User> {

    /**
     * 创建用户，教师只能创建学生，管理员可以创建教师和学生
     *
     * @param userForm
     * @return
     */
    Result<String> createUser(UserForm userForm);

    /**
     * 编辑用户，可修改真实姓名、学生/教师班级（多选）
     *
     * @param id       用户ID
     * @param userForm 用户信息
     * @return 响应结果
     */
    Result<String> updateUser(Integer id, UserForm userForm);

    /**
     * 个人中心修改真实姓名（用户名不可改）
     *
     * @param userForm 仅使用 realName
     * @return 响应结果
     */
    Result<String> updateProfile(UserForm userForm);

    /**
     * 用户修改密码
     *
     * @param userForm
     * @return
     */
    Result<String> updatePassword(UserForm userForm);

    /**
     * 批量删除用户
     *
     * @param ids 删除用用户id
     * @return
     */
    Result<String> deleteBatchByIds(String ids);

    /**
     * Excel导入用户数据
     *
     * @param file 用户数据excel
     * @return
     */
    Result<String> importUsers(MultipartFile file);

    /**
     * 获取用户个人信息
     *
     * @return 响应结果
     */
    Result<UserVO> info();

    /**
     * 用户加入班级，只有学生才能加入班级
     *
     * @param code 班级代码
     * @return
     */
    Result<String> joinGrade(String code);

    /**
     * 教师和管理员 用户管理 分页获取用户信息
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param gradeId  班级ID
     * @param realName 真实姓名
     * @return
     */
    Result<IPage<UserVO>> pagingUser(Integer pageNum, Integer pageSize, Integer gradeId, String realName);


    /**
     * 用户上传头像
     *
     * @param file 头像文件
     * @return 图片地址
     */
    Result<String> uploadAvatar(MultipartFile file);

    /**
     * 恢复默认头像（清空自定义头像）
     */
    Result<String> resetAvatar();

}
