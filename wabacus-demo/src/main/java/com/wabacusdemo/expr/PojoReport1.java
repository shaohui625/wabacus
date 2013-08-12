package com.wabacusdemo.expr;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import java.util.Date;

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
}

/* Location:           R:\tomcat\webapps\WabacusDemo\WEB-INF\classes\
 * Qualified Name:     com.wabacus.generateclass.Pojo_test110report1
 * JD-Core Version:    0.6.2
 */