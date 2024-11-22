package com.lxy.sean.j2ts;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.lxy.sean.j2ts.utils.CommonUtils;
import com.lxy.sean.j2ts.utils.IdeaUtils;
import com.lxy.sean.j2ts.utils.TypescriptUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class JavaBeanToTypescriptInterfaceAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // get java file
        VirtualFile[] virtualFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles == null || virtualFiles.length != 1) {
            Messages.showInfoMessage("Please select a Java Bean", "");
            return;
        }
        VirtualFile target = virtualFiles[0];
        if (target.isDirectory()) {
            Messages.showInfoMessage("Please select a Java Bean file", "");
            return;
        }
        PsiFile file = PsiManager.getInstance(project).findFile(target);
        if (!(file instanceof PsiJavaFile)) {
            Messages.showInfoMessage("Please select a Java Bean file !", "");
            return;
        }

        if (!(file instanceof PsiJavaFileImpl)) {
            Messages.showMessageDialog(project,
                    "The selected is not a Java source file, so document (or other info) may be lost",
                    "Warning",
                    Messages.getWarningIcon());
        }

        PsiJavaFile psiJavaFile = (PsiJavaFile) file;

        // convert to ts
        String interfaceContent = TypescriptUtils.generatorInterfaceContentForPsiJavaFile(project, psiJavaFile);

        int chooseAfterGenerate = Messages.showYesNoDialog(project,
                "Please choose to copy or save the generated result.", "Warning",
                "Copy to Clipboard",
                "Save to File",
                Messages.getInformationIcon());
        if (chooseAfterGenerate == Messages.YES) {
            // copy to Clip
            IdeaUtils.copyToClipboard(interfaceContent);
            IdeaUtils.showNotification(project, NotificationType.INFORMATION,
                    "Success",
                    "Copy to clipboard");
        } else {
            // save to file
            doSaveToFile(project, psiJavaFile, interfaceContent);
        }
    }

    private static void doSaveToFile(Project project, PsiJavaFile psiJavaFile, String interfaceContent) {
        FileChooserDescriptor chooserDescriptor = CommonUtils.createFileChooserDescriptor("Choose a folder to save result as a file, which ends with '.d.ts'",
                "You can copy result from clipboard without choosing any.");
        VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);
        if (savePathFile != null && savePathFile.isDirectory()) {
            String savePath = savePathFile.getPath();
            String nameWithoutExtension = psiJavaFile.getVirtualFile().getNameWithoutExtension();
            String interfaceFileSavePath = savePath + "/" + nameWithoutExtension + ".d.ts";
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(interfaceFileSavePath, false), StandardCharsets.UTF_8));
                bufferedWriter.write(interfaceContent);
                bufferedWriter.close();

                IdeaUtils.showNotification(project, NotificationType.INFORMATION,
                        "success",
                        "The target file was saved to:  " + interfaceFileSavePath);
            } catch (IOException ioException) {
                // pass
            }
        }
    }
}
