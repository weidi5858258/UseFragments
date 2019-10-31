package com.weidi.usefragments.business.medical_record;

import com.weidi.dbutil.ClassVersion;
import com.weidi.dbutil.Primary;

/***
 Created by root on 19-10-31.
 */
@ClassVersion(version = 0)
public class MedicalRecordBean {
    @Primary
    public int _id;

    // 日期
    public String medicalRecordDate;
    // 白细胞计数
    public String medicalRecordLeukocyteCount;
    // 嗜中性粒细胞绝对值
    public String medicalRecordNeutrophils;
    // 血红蛋白
    public String medicalRecordHemoglobin;
    // 血小板计数
    public String medicalRecordPlateletCount;
    // 备注
    public String medicalRecordRemarks;
    // 其他
    public String medicalRecordOther;

    @Override
    public String toString() {
        return "MedicalRecordBean{" +
                "_id=" + _id +
                ", medicalRecordDate='" + medicalRecordDate + '\'' +
                ", medicalRecordLeukocyteCount='" + medicalRecordLeukocyteCount + '\'' +
                ", medicalRecordNeutrophils='" + medicalRecordNeutrophils + '\'' +
                ", medicalRecordHemoglobin='" + medicalRecordHemoglobin + '\'' +
                ", medicalRecordPlateletCount='" + medicalRecordPlateletCount + '\'' +
                ", medicalRecordRemarks='" + medicalRecordRemarks + '\'' +
                ", medicalRecordOther='" + medicalRecordOther + '\'' +
                '}';
    }
}
