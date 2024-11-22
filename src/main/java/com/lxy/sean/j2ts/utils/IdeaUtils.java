package com.lxy.sean.j2ts.utils;


import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;

import java.awt.datatransfer.StringSelection;

public class IdeaUtils {
    public static void showNotification(Project project, NotificationType type, String title , String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("J2TS Notification Group")
                .createNotification(content, type)
                .setTitle(title)
                .notify(project);
    }

    public static void copyToClipboard(String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }

}