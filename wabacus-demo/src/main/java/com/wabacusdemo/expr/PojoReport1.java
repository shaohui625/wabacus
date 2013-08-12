package com.wabacusdemo.expr;

import java.util.Date;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;

public class PojoReport1 extends AbsReportDataPojo
{
  private String guid;
  private String deptno;
  public String getDeptno()
{
    return deptno;
}

public void setDeptno(String deptno)
{
    this.deptno=deptno;
    this.setDynamicColData("deptno",deptno);
}

private String deptname;
  private String manager;
  private Date builtdate;
  private String fromdb110;
  private String performance;

  public PojoReport1(ReportRequest paramReportRequest, ReportBean paramReportBean)
  {
    super(paramReportRequest, paramReportBean);
  }

  public void setGuid(String paramString)
  {
    this.guid = paramString;
  }

  public String getGuid()
  {
    return this.guid;
  }

  public void setDeptname(String paramString)
  {
    this.deptname = paramString;

    this.setDynamicColData("deptname",paramString);
  }

  public String getDeptname()
  {
    return this.deptname;
  }

  public void setManager(String paramString)
  {
    this.manager = paramString;
  }

  public String getManager()
  {
    return this.manager;
  }

  public void setBuiltdate(Date paramDate)
  {
    this.builtdate = paramDate;
  }

  public Date getBuiltdate()
  {
    return this.builtdate;
  }

  public void setFromdb110(String paramString)
  {
    this.fromdb110 = paramString;
  }

  public String getFromdb110()
  {
    return this.fromdb110;
  }

  public void setPerformance(String paramString)
  {
    this.performance = paramString;
  }

  public String getPerformance()
  {
    return this.performance;
  }

 
public Object getColValue(ColBean cbean)
{
    if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return null;
    if(cbean.isNonValueCol()||cbean.isSequenceCol()||cbean.isControlCol()) return null;
    if("[DYN_COL_DATA]".equals(cbean.getProperty()))
    {
        return getDynamicColData(cbean.getColumn());
    }else
    {
        try
        {
            return cbean.getGetMethod().invoke(this,new Object[] {});
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("从POJO中获取报表"+cbean.getReportBean().getPath()+"的列"+cbean.getColumn()+"数据失败",e);
        }
    }
}
}

/* Location:           R:\tomcat\webapps\WabacusDemo\WEB-INF\classes\
 * Qualified Name:     com.wabacus.generateclass.Pojo_test110report1
 * JD-Core Version:    0.6.2
 */