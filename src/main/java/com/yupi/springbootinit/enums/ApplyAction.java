package com.yupi.springbootinit.enums;

public enum ApplyAction {
    CREATE, // 新增
    UPDATE,  // 编辑
    SUBMIT,  //提交
    CANCEL, //取消
    DRAFT_SAVE, //保存草稿
    DRAFT_CREATE, //新建草稿
    RESUBMIT, //被驳回重新提交
    DELETE //删除草稿
}
