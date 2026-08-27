package com.xiaozhi.server.web;

import com.xiaozhi.server.web.PageFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * @description: Controller base
 *
 * @author Joey
 *
 */
public class BaseController {

    /**
     * Quantidade máxima de itens por página
     */
    public static final int MAX_PAGE_SIZE = 1000;

    protected PageFilter initPageFilter(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String pageNo = request.getParameter("pageNo");
        String pageSize = request.getParameter("pageSize");
        if (!StringUtils.hasText(pageNo) && !StringUtils.hasText(pageSize)) {
            return null;
        }

        PageFilter pageFilter = new PageFilter();
        if (StringUtils.hasText(pageNo)) {
            pageFilter.setStart(Math.max(Integer.parseInt(pageNo), 1));
        }
        if (StringUtils.hasText(pageSize)) {
            int pageSizeValue = Math.min(Math.max(Integer.parseInt(pageSize), 1), MAX_PAGE_SIZE);
            pageFilter.setLimit(pageSizeValue);
        }
        return pageFilter;
    }
}
