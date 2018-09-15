package com.duan.blogos.api.blogger;

import com.duan.blogos.service.dto.blogger.BloggerCategoryDTO;
import com.duan.blogos.service.exception.CodeMessage;
import com.duan.blogos.service.exception.ResultUtil;
import com.duan.blogos.service.restful.ResultModel;
import com.duan.blogos.service.service.blogger.BloggerCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * Created on 2018/1/11.
 * 博主博文类别
 * <p>
 * 1 查看所有类别
 * 2 查看指定类别
 * 3 增加类别
 * 4 修改类别
 * 5 删除类别
 *
 * @author DuanJiaNing
 */
@RestController
@RequestMapping("/blogger/{bloggerId}/category")
public class BloggerBlogCategoryController extends BaseBloggerController {

    @Autowired
    private BloggerCategoryService bloggerCategoryService;

    /**
     * 查看所有类别
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResultModel<List<BloggerCategoryDTO>> list(HttpServletRequest request,
                                                      @PathVariable Integer bloggerId,
                                                      @RequestParam(value = "offset", required = false) Integer offset,
                                                      @RequestParam(value = "rows", required = false) Integer rows) {
        handleAccountCheck(request, bloggerId);

        int os = offset == null || offset < 0 ? 0 : offset;
        int rs = rows == null || rows < 0 ? bloggerProperties.getRequestBloggerBlogCategoryCount() : rows;
        ResultModel<List<BloggerCategoryDTO>> result = bloggerCategoryService.listBlogCategory(bloggerId, os, rs);
        if (result == null) handlerEmptyResult();

        return result;
    }


    /**
     * 查看指定类别
     */
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.GET)
    public ResultModel<BloggerCategoryDTO> get(HttpServletRequest request,
                                               @PathVariable Integer bloggerId,
                                               @PathVariable Integer categoryId) {
        handleAccountCheck(request, bloggerId);
        handleCategoryExistCheck(request, bloggerId, categoryId);

        BloggerCategoryDTO dto = bloggerCategoryService.getCategory(bloggerId, categoryId);
        if (dto == null) handlerOperateFail();

        return new ResultModel<>(dto);
    }


    /**
     * 增加类别
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResultModel add(HttpServletRequest request,
                           @PathVariable Integer bloggerId,
                           @RequestParam(value = "iconId", required = false) Integer iconId,
                           @RequestParam("title") String title,
                           @RequestParam(value = "bewrite", required = false) String bewrite) {

        // 检查博主是否登录
        handleBloggerSignInCheck(request, bloggerId);
        handlePictureExistCheck(request, bloggerId, iconId);

        if (StringUtils.isEmpty(title))
            throw ResultUtil.failException(CodeMessage.COMMON_PARAMETER_ILLEGAL);

        int id = bloggerCategoryService.insertBlogCategory(bloggerId, iconId == null ? -1 : iconId, title, bewrite);
        if (id < 0) handlerOperateFail();

        return new ResultModel<>(id);
    }

    /**
     * 修改类别
     */
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.PUT)
    public ResultModel update(HttpServletRequest request,
                              @PathVariable Integer bloggerId,
                              @PathVariable Integer categoryId,
                              @RequestParam(value = "iconId", required = false) Integer newIconId,
                              @RequestParam(value = "title", required = false) String newTitle,
                              @RequestParam(value = "bewrite", required = false) String newBewrite) {

        handleParamAllNullForUpdate(request, newIconId, newTitle, newBewrite);
        handleBloggerSignInCheck(request, bloggerId);
        handleCategoryExistCheck(request, bloggerId, categoryId);
        handlePictureExistCheck(request, bloggerId, newIconId);

        if (!bloggerCategoryService.updateBlogCategory(bloggerId, categoryId, Optional.ofNullable(newIconId).orElse(-1),
                newTitle, newBewrite))
            handlerOperateFail();

        return new ResultModel<>("");
    }

    // 检查指定博主是否有指定的博文类别
    private void handleCategoryExistCheck(HttpServletRequest request, int bloggerId, int categoryId) {
        if (!bloggerValidateService.checkBloggerBlogCategoryExist(bloggerId, categoryId))
            throw ResultUtil.failException(CodeMessage.COMMON_UNKNOWN_CATEGORY);
    }

    /**
     * 删除类别，类别被删除后该类别下的所有博文将被移动到指定类别，不指定将移动到默认类别。
     * 不能同时删除类别下的所有文章，删除博文通过博文api操控。
     */
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.DELETE)
    public ResultModel delete(HttpServletRequest request,
                              @PathVariable Integer bloggerId,
                              @PathVariable Integer categoryId,
                              @RequestParam(value = "newCategoryId", required = false) Integer newCategoryId) {

        handleBloggerSignInCheck(request, bloggerId);
        handleCategoryExistCheck(request, bloggerId, categoryId);

        int cate = -1;
        if (newCategoryId != null) {

            //检查删除类别和原博文移动到类别是否相同
            if (newCategoryId.equals(categoryId))
                throw ResultUtil.failException(CodeMessage.COMMON_PARAMETER_ILLEGAL);

            //检查新类别
            handleCategoryExistCheck(request, bloggerId, newCategoryId);

            cate = newCategoryId;
        }

        if (!bloggerCategoryService.deleteCategoryAndMoveBlogsTo(bloggerId, categoryId, cate))
            handlerOperateFail();

        return new ResultModel<>("");
    }


}