package com.base.utils.http;

/**
 * <功能详细描述>
 *
 * @author XXX
 * @version [版本号, 2016/6/8]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public interface ITRequestResult<T> {

    public void onSuccessful(T entity,String session);

    public void onFailure(String errorMsg);

    public void onCompleted();

    public void sessionLose();
}