/* 
 * Copyright (C) 2010---2013 星星(wuweixing)<349446658@qq.com>
 * 
 * This file is part of Wabacus 
 * 
 * Wabacus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wabacus.system.dataimport.thread;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.system.dataimport.DataImportItem;
import com.wabacus.system.dataimport.queue.UploadFilesQueue;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.FileLockTools;
import com.wabacus.util.Tools;

public class FileUpDataImportThread extends AbsDataImportThread
{
    private static Log log=LogFactory.getLog(FileUpDataImportThread.class);

    private final static FileUpDataImportThread instance=new FileUpDataImportThread();

    private FileUpDataImportThread()
    {}

    public static FileUpDataImportThread getInstance()
    {
        return instance;
    }

    public void stopRunning()
    {
        super.stopRunning();
        UploadFilesQueue.getInstance().notifyAllThread();
    }

    public void run()
    {
        while(RUNNING_FLAG)
        {
            try
            {
                List<Map<List<DataImportItem>,Map<File,FileItem>>> lstUploadFiles=UploadFilesQueue
                        .getInstance().getLstAllUploadFiles();
                log.debug("上传文件线程启动，正在进行文件上传.........................");
                String lockfile;
                for(Map<List<DataImportItem>,Map<File,FileItem>> mUploadFilesTmp:lstUploadFiles)
                {
                    if(mUploadFilesTmp.size()==0) continue;
                    Entry<List<DataImportItem>,Map<File,FileItem>> entry=mUploadFilesTmp.entrySet()
                            .iterator().next();
                    List<DataImportItem> lstDiitems=entry.getKey();
                    lockfile=Tools.standardFilePath(lstDiitems.get(0).getConfigBean().getFilepath()
                            +"\\"+Consts_Private.DATAIMPORT_LOCKFILENAME);
                    Map<File,FileItem> mFiles=entry.getValue();
                    if(mFiles==null||mFiles.size()==0) continue;
                    Object lockresource=FileLockTools.lock(lockfile,10,100);
                    if(lockresource==null)
                    {
                        log.error("获取文件锁"+lockfile+"失败，无法进行数据导入");
                        break;
                    }
                    try
                    {
                        for(Entry<File,FileItem> fileTmpEntry:mFiles.entrySet())
                        {
                            if(fileTmpEntry==null) continue;
                            File f=fileTmpEntry.getKey();
                            FileItem fitem=fileTmpEntry.getValue();
                            try
                            {
                                fitem.write(f);
                            }catch(Exception e)
                            {
                                log.error("上传数据文件"+f.getAbsolutePath()+"失败",e);
                                continue;
                            }
                        }
                        for(DataImportItem diitemTmp:lstDiitems)
                        {
                            diitemTmp.doImportData();
                        }
                    }catch(Exception e)
                    {
                        log.error("导入数据失败",e);
                    }finally
                    {
                        try
                        {
                            for(DataImportItem diitemTmp:lstDiitems)
                            {
                                diitemTmp.backupOrDeleteDataFile();
                            }
                        }catch(Exception e)
                        {
                            log.error("备份或删除数据文件失败",e);
                        }
                        FileLockTools.unlock(lockfile,lockresource);
                    }
                }
            }catch(Exception e)
            {
                log.error("数据导入线程运行失败",e);
            }
        }
    }
}
