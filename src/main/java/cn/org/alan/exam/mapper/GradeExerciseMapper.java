package cn.org.alan.exam.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cn.org.alan.exam.model.entity.GradeExercise;

/**
 * 班级刷题表 Mapper 接口
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface GradeExerciseMapper extends BaseMapper<GradeExercise> {

    /**
     * 批量添加题库与班级关联
     *
     * @param repoId      题库ID
     * @param gradeIdList 班级ID列表
     * @param userId      创建人ID
     * @return 添加记录数
     */
    int addRepoGrade(@Param("repoId") Integer repoId,
                     @Param("gradeIdList") List<Integer> gradeIdList,
                     @Param("userId") Integer userId);

    /**
     * 删除题库的全部班级关联
     *
     * @param repoId 题库ID
     * @return 删除记录数
     */
    int delRepoGrade(@Param("repoId") Integer repoId);

    /**
     * 删除班级的全部题库关联
     *
     * @param gradeId 班级ID
     * @return 删除记录数
     */
    int deleteByGradeId(@Param("gradeId") Integer gradeId);

    /**
     * 获得题库绑定的班级ID列表
     *
     * @param repoId 题库ID
     * @return 班级ID列表
     */
    List<Integer> getGradeList(@Param("repoId") Integer repoId);

    /**
     * 获得班级绑定的题库ID列表
     *
     * @param gradeId 班级ID
     * @return 题库ID列表
     */
    List<Integer> getRepoIdList(@Param("gradeId") Integer gradeId);

    /**
     * 按题库ID批量查询关联
     *
     * @param repoIds 题库ID列表
     * @return 关联列表
     */
    List<GradeExercise> selectByRepoIds(@Param("repoIds") List<Integer> repoIds);

    /**
     * 校验学生班级是否可刷该题库（已绑定且题库开启刷题、未删除）
     *
     * @param repoId  题库ID
     * @param gradeId 班级ID
     * @return 命中条数
     */
    int countStudentRepoAccess(@Param("repoId") Integer repoId, @Param("gradeId") Integer gradeId);

}
