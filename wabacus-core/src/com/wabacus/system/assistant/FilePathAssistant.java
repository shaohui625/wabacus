/* 
 * Copyright (C) 2010---2012 星星(wuweixing)<349446658@qq.com>
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
package com.wabacus.system.assistant;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.util.RegexTools;
import com.wabacus.util.Tools;

public class FilePathAssistant
{
    private final static FilePathAssistant instance=new FilePathAssistant();

    private FilePathAssistant()
    {}

    public static FilePathAssistant getInstance()
    {
        return instance;
    }

    public void getLstFilesByPath(List<String> lstResults,String filepath,boolean isClasspath,boolean isRecursive)
    {
        if(filepath==null) return;
        filepath=filepath.trim();
        String path=null, filenamePattern=filepath;
        if(isClasspath)
        {
            List<String> lstTmp=null;
            if(filepath.equals("")||filepath.equals("*")||filepath.equals("/")||filepath.equals("/*"))
            {
                lstTmp=getListFilesFromClasspath("/","*",isRecursive);
            }else
            {
                int idx=filepath.lastIndexOf("/");
                if(idx>0)
                {
                    path=filepath.substring(0,idx).trim();
                    filenamePattern=filepath.substring(idx+1).trim();
                }
                if(path==null) path="";
                if(filenamePattern==null||filenamePattern.trim().equals("")) filenamePattern="*";
                lstTmp=getListFilesFromClasspath(path,filenamePattern,isRecursive);
            }
            if(lstTmp!=null&&lstTmp.size()>0)
            {
                for(String filePathTmp:lstTmp)
                {
                    filePathTmp="classpath{"+filePathTmp+"}";
                    if(!lstResults.contains(filePathTmp)) lstResults.add(filePathTmp);
                }
            }
        }else
        {
            if(filepath.equals("")) return;
            int idx=filepath.lastIndexOf(File.separator);
            if(idx>0)
            {
                path=filepath.substring(0,idx+1);
                filenamePattern=filepath.substring(idx+1);
            }
            if(path==null||path.trim().equals(""))
            {
                if(!lstResults.contains(filenamePattern)) lstResults.add(filenamePattern);
            }else
            {
                getFilesFromDir(path,filenamePattern,isRecursive,lstResults);
            }
        }
    }

    private void getFilesFromDir(String parentFilePath,final String regex,final boolean recursive,List<String> lstResults)
    {
        File parentDirObj=new File(parentFilePath);
        if(!parentDirObj.exists()||!parentDirObj.isDirectory()) return;
        File[] childFiles=parentDirObj.listFiles(new FileFilter()
        {// 自定义过滤规则 如果可以循环(包含子目录)
            public boolean accept(File file)
            {
                if(file.isDirectory()) return recursive;
                if(regex==null||regex.trim().equals("")||regex.trim().equals("*")) return true;
                return RegexTools.isMatch(file.getName(),regex);
            }
        });
        String filePathTmp;
        for(File fileTmp:childFiles)
        {
            if(fileTmp.isDirectory())
            {
                getFilesFromDir(fileTmp.getAbsolutePath(),regex,recursive,lstResults);
            }else
            {
                filePathTmp=fileTmp.getAbsolutePath();
                if(!lstResults.contains(filePathTmp)) lstResults.add(filePathTmp);
            }
        }
    }
    
    public List<String> getListFilesFromClasspath(String rootpath,String regex,boolean isRecursive)
    {
        if(rootpath==null) return null;
        rootpath=Tools.replaceAll(rootpath.trim(),"\\","/");
        rootpath=Tools.replaceAll(rootpath.trim(),"//","/");
        while(rootpath.startsWith("/"))
            rootpath=rootpath.substring(1);
        if(rootpath.equals("/")) rootpath="";
        rootpath=rootpath.trim();
        if(!rootpath.equals("")&&!rootpath.endsWith("/")) rootpath=rootpath+"/";
        List<String> lstResults=new ArrayList<String>();
        try
        {
            Enumeration<URL> rootUrls=ConfigLoadManager.currentDynClassLoader.getResources(rootpath);
            URL urlTmp;
            String protocolTmp;
            while(rootUrls.hasMoreElements())
            {
                urlTmp=(URL)rootUrls.nextElement();
                protocolTmp=urlTmp.getProtocol();
                if("file".equals(protocolTmp) || "vfs".equals(protocolTmp))  // "vfs" for jboss AS
                {//是目录
                    String filePath=URLDecoder.decode(urlTmp.getFile(),"UTF-8");
                    if("vfs".equals(protocolTmp) ){
                    	int indexOf = filePath.indexOf("/WEB-INF");
                    	 if(indexOf > 0 && !new File(filePath).exists()){
                    		 filePath =  Config.webroot_abspath+filePath.substring(indexOf);
                    	 }
                    }
                    getFileFromClasspathDir(rootpath,filePath,regex,isRecursive,lstResults);
                }else if("jar".equals(protocolTmp))
                {
                    JarFile jar=((JarURLConnection)urlTmp.openConnection()).getJarFile();
                    Enumeration<JarEntry> entries=jar.entries();
                    JarEntry jarEntryTmp;
                    while(entries.hasMoreElements())
                    {
                        jarEntryTmp=entries.nextElement();// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                        if(jarEntryTmp.isDirectory()) continue;
                        String name=jarEntryTmp.getName();
                        if(name.charAt(0)=='/') name=name.substring(1);// 如果是以/开头的
                        
                        if(isValidFileInJar(rootpath,name,isRecursive))
                        {
                            String filenameTmp=name;
                            int idx=name.lastIndexOf('/');
                            if(idx>0) filenameTmp=filenameTmp.substring(idx+1).trim();
                            if(regex==null||regex.trim().equals("")||regex.trim().equals("*")||RegexTools.isMatch(filenameTmp,regex))
                            {
                                if(!lstResults.contains(name)) lstResults.add(name);
                            }
                        }
                    }
                }
            }
        }catch(IOException e)
        {
            throw new WabacusConfigLoadingException("从路径"+rootpath+"中读取"+regex+"文件失败",e);
        }
        return lstResults;
    }

    private boolean isValidFileInJar(String rootpath,String filepath,boolean recursive)
    {
        if(rootpath==null||rootpath.trim().equals(""))
        {
            if(recursive)
            {//如果是递归获取根目录下面的文件，则无条件返回true
                return true;
            }else
            {
                if(filepath.indexOf("/")>=0) return false;
            }
        }else
        {
            if(filepath.indexOf(rootpath)!=0) return false;
            if(!recursive)
            {
                filepath=filepath.substring(rootpath.length());
                if(filepath.equals("")||filepath.indexOf("/")>=0) return false;
            }
        }
        return true;
    }

    private void getFileFromClasspathDir(String rootpackage,String parentFilePath,final String regex,final boolean recursive,List<String> lstResults)
    {
        File parentDirObj=new File(parentFilePath);
        if(!parentDirObj.exists()||!parentDirObj.isDirectory()) return;
        File[] childFiles=parentDirObj.listFiles(new FileFilter()
        {
            public boolean accept(File file)
            {
                if(file.isDirectory()) return recursive;
                if(regex==null||regex.trim().equals("")||regex.trim().equals("*")) return true;//如果没有指定正则表达式，或指定为*，则匹配所有文件
                return RegexTools.isMatch(file.getName(),regex);
            }
        });
        rootpackage=rootpackage.trim();
        if(!rootpackage.equals("")&&!rootpackage.endsWith("/")) rootpackage=rootpackage+"/";
        String filePathTmp;
        for(File fileTmp:childFiles)
        {
            if(fileTmp.isDirectory())
            {
                getFileFromClasspathDir(rootpackage+fileTmp.getName(),fileTmp.getAbsolutePath(),regex,recursive,lstResults);
            }else
            {
                filePathTmp=rootpackage+fileTmp.getName();
                if(!lstResults.contains(filePathTmp)) lstResults.add(filePathTmp);
            }
        }
    }
}
