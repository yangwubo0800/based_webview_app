package com.base.bean;

/**
 * <网络请求返回体>
 *
 * @author XXX
 * @version [版本号, 2016/6/6]
 * @see [相关类/方法]
 * @since [V1]
 */
public class BaseResp
{
    /**
     * 返回状态码
     */
    protected int retcode;

    /**
     * 返回信息描述
     */
    protected String retinfo;

    public int getRetcode()
    {
        return retcode;
    }

    public void setRetcode(int retcode)
    {
        this.retcode = retcode;
    }

    public String getRetinfo()
    {
        return retinfo;
    }

    public void setRetinfo(String retinfo)
    {
        this.retinfo = retinfo;
    }

/*	protected boolean success;
	protected String msg;
	public int getRetcode()
    {
        return 000000;
    }

    public void setRetcode(Boolean success)
    {
        this.success = success;
    }

    public String getRetinfo()
    {
        return msg;
    }

    public void setRetinfo(String msg)
    {
        this.msg = msg;
    }*/

/*	protected boolean success;
	protected String msg;
	protected Object obj;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Object getObj() {
		return obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}*/

}