package com.ruoyi.framework.web.domain;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.*;
import cn.hutool.system.oshi.CpuInfo;
import cn.hutool.system.oshi.OshiUtil;
import com.ruoyi.framework.web.domain.server.*;
import lombok.Data;
import oshi.hardware.GlobalMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.util.LinkedList;
import java.util.List;

/**
 * 服务器相关信息
 *
 * @author ruoyi
 */
@Data
public class Server {
    private static final int OSHI_WAIT_SECOND = 1000;

    /**
     * CPU相关信息
     */
    private Cpu cpu = new Cpu();

    /**
     * 內存相关信息
     */
    private Mem mem = new Mem();

    /**
     * JVM相关信息
     */
    private Jvm jvm = new Jvm();

    /**
     * 服务器相关信息
     */
    private Sys sys = new Sys();

    /**
     * 磁盘相关信息
     */
    private List<SysFile> sysFiles = new LinkedList<SysFile>();


    public void copyTo() {
        setCpuInfo();
        setMemInfo();
        setSysInfo();
        setJvmInfo();
        setSysFiles();
    }

    /**
     * 设置CPU信息
     */
    private void setCpuInfo() {
        CpuInfo cpuInfo = OshiUtil.getCpuInfo(OSHI_WAIT_SECOND);
        cpu.setCpuNum(cpuInfo.getCpuNum());
        cpu.setTotal(cpuInfo.getToTal());
        cpu.setSys(cpuInfo.getSys());
        cpu.setUsed(cpuInfo.getUsed());
        cpu.setWait(cpuInfo.getWait());
        cpu.setFree(cpuInfo.getFree());
    }

    /**
     * 设置内存信息
     */
    private void setMemInfo() {
        GlobalMemory memory = OshiUtil.getMemory();
        mem.setTotal(memory.getTotal());
        mem.setUsed(memory.getTotal() - memory.getAvailable());
        mem.setFree(memory.getAvailable());
    }

    /**
     * 设置服务器信息
     */
    private void setSysInfo() {
        HostInfo hostInfo = SystemUtil.getHostInfo();
        OsInfo osInfo = SystemUtil.getOsInfo();
        UserInfo userInfo = SystemUtil.getUserInfo();
        sys.setComputerName(hostInfo.getName());
        sys.setComputerIp(hostInfo.getAddress());
        sys.setOsName(osInfo.getName());
        sys.setOsArch(osInfo.getArch());
        sys.setUserDir(userInfo.getCurrentDir());
    }

    /**
     * 设置Java虚拟机
     */
    private void setJvmInfo() {
        JavaInfo javaInfo = SystemUtil.getJavaInfo();
        RuntimeInfo runtimeInfo = SystemUtil.getRuntimeInfo();
        JavaRuntimeInfo javaRuntimeInfo = SystemUtil.getJavaRuntimeInfo();
        jvm.setTotal(runtimeInfo.getTotalMemory());
        jvm.setMax(runtimeInfo.getMaxMemory());
        jvm.setFree(runtimeInfo.getFreeMemory());
        jvm.setVersion(javaInfo.getVersion());
        jvm.setHome(javaRuntimeInfo.getHomeDir());
    }

    /**
     * 设置磁盘信息
     */
    private void setSysFiles() {
        OperatingSystem os = OshiUtil.getOs();
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> fsArray = fileSystem.getFileStores();
        for (OSFileStore fs : fsArray) {
            long free = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            long used = total - free;
            SysFile sysFile = new SysFile();
            sysFile.setDirName(fs.getMount());
            sysFile.setSysTypeName(fs.getType());
            sysFile.setTypeName(fs.getName());
            sysFile.setTotal(convertFileSize(total));
            sysFile.setFree(convertFileSize(free));
            sysFile.setUsed(convertFileSize(used));
            sysFile.setUsage(NumberUtil.mul(NumberUtil.div(used, total, 4), 100));
            sysFiles.add(sysFile);
        }
    }

    /**
     * 字节转换
     *
     * @param size 字节大小
     * @return 转换后值
     */
    public String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else {
            return String.format("%d B", size);
        }
    }
}
