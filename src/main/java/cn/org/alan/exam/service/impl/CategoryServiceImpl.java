package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.cache.CacheKeys;
import cn.org.alan.exam.common.cache.CacheService;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.CategoryMapper;
import cn.org.alan.exam.model.entity.Category;
import cn.org.alan.exam.model.vo.category.CategoryVO;
import cn.org.alan.exam.service.ICategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 *
 * @author Moxuec
 * @since 2025-04-09
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    private static final long CATEGORY_TTL_HOURS = 12;

    @Resource
    private CacheService cacheService;

    @Override
    public Result<String> addCategory(Category category) {
        category.setCreateTime(new Date());
        save(category);
        evictCategoryCache();
        return Result.success("添加分类成功");
    }

    @Override
    public Result<String> updateCategory(Category category, Integer id) {
        category.setId(id);
        updateById(category);
        evictCategoryCache();
        return Result.success("修改分类成功");
    }

    @Override
    public Result<String> deleteCategory(Integer id) {
        // 检查是否有子分类
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, id);
        long count = count(wrapper);
        if (count > 0) {
            return Result.failed("该分类下有子分类，不能删除");
        }
        
        // 检查是否有题库使用该分类
        // 这里需要添加检查题库是否使用该分类的逻辑
        
        removeById(id);
        evictCategoryCache();
        return Result.success("删除分类成功");
    }

    @Override
    public Result<List<CategoryVO>> getCategoryTree() {
        List<CategoryVO> cached = cacheService.get(CacheKeys.CATEGORY_TREE);
        if (cached != null) {
            return Result.success("获取分类树成功", cached);
        }
        // 获取所有分类
        List<Category> allCategories = list();
        
        // 构建分类树
        List<CategoryVO> result = buildCategoryTree(allCategories, 0);
        cacheService.set(CacheKeys.CATEGORY_TREE, result, CATEGORY_TTL_HOURS, TimeUnit.HOURS);
        return Result.success("获取分类树成功", result);
    }

    @Override
    public Result<List<CategoryVO>> getFirstLevelCategories() {
        List<CategoryVO> cached = cacheService.get(CacheKeys.CATEGORY_FIRST);
        if (cached != null) {
            return Result.success("获取一级分类成功", cached);
        }
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, 0)
               .orderByAsc(Category::getSort);
        
        List<Category> categories = list(wrapper);
        List<CategoryVO> result = categories.stream().map(this::convertToVO).collect(Collectors.toList());
        cacheService.set(CacheKeys.CATEGORY_FIRST, result, CATEGORY_TTL_HOURS, TimeUnit.HOURS);
        return Result.success("获取一级分类成功", result);
    }

    @Override
    public Result<List<CategoryVO>> getChildCategories(Integer parentId) {
        String key = CacheKeys.categoryChildren(parentId);
        List<CategoryVO> cached = cacheService.get(key);
        if (cached != null) {
            return Result.success("获取子分类成功", cached);
        }
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, parentId)
               .orderByAsc(Category::getSort);
        
        List<Category> categories = list(wrapper);
        List<CategoryVO> result = categories.stream().map(this::convertToVO).collect(Collectors.toList());
        cacheService.set(key, result, CATEGORY_TTL_HOURS, TimeUnit.HOURS);
        return Result.success("获取子分类成功", result);
    }

    /**
     * 分类变更后清除相关缓存
     */
    private void evictCategoryCache() {
        cacheService.delete(CacheKeys.CATEGORY_TREE);
        cacheService.delete(CacheKeys.CATEGORY_FIRST);
        cacheService.deleteByPrefix(CacheKeys.CATEGORY_CHILDREN_PREFIX);
    }
    
    /**
     * 构建分类树
     *
     * @param categories 所有分类
     * @param parentId 父ID
     * @return 分类树
     */
    private List<CategoryVO> buildCategoryTree(List<Category> categories, Integer parentId) {
        List<CategoryVO> result = new ArrayList<>();
        
        for (Category category : categories) {
            if (category.getParentId().equals(parentId)) {
                CategoryVO vo = convertToVO(category);
                vo.setChildren(buildCategoryTree(categories, category.getId()));
                result.add(vo);
            }
        }
        
        return result;
    }
    
    /**
     * 将实体转换为VO
     *
     * @param category 分类实体
     * @return 分类VO
     */
    private CategoryVO convertToVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
