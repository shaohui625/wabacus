package com.wabacus.extra.buttons;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.buttons.WabacusButton;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;

public class ShowPageButton extends WabacusButton {

    public ShowPageButton(IComponentConfigBean ccbean) {
        super(ccbean);
    }
    
    public String getButtonType()
    {
        return Consts.IMPORT_DATA;
    }

    public String showButton(ReportRequest rrequest,String dynclickevent)
    {
        return super.showButton(rrequest,getDataImportEvent());
    }

    public String showButton(ReportRequest rrequest,String dynclickevent,String button)
    {
        return super.showButton(rrequest,getDataImportEvent(),button);
    }
    
    private String getDataImportEvent()
    {
        String token="?";
        if(Config.showreport_url.indexOf("?")>0) token="&";
        String serverurl=Config.showreport_url+token+"&DISPLAY_TYPE=1&PAGEID="+ccbean.getPageBean().getId()+"&COMPONENTID="+ccbean.getId()
                +"&ACTIONTYPE=ShowUploadFilePage&FILEUPLOADTYPE="+Consts_Private.FILEUPLOADTYPE_DATAIMPORTREPORT;
        serverurl+="&DATAIMPORT_BUTTONNAME="+this.getName();
        //return "wx_winpage('"+serverurl+"',"+this.dataimportBean.getDataimportpopupparams()+");";
        return null ;//FIXME
    }

    public void loadExtendConfig(XmlElementBean eleButtonBean)
    {
        super.loadExtendConfig(eleButtonBean);
        
    }


}
