package com.lxy.sean.j2ts.utils;

import com.intellij.lang.jvm.JvmClassKind;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TypescriptUtils {

    /**
     *
     */
    private static final Map<String, Integer> canonicalText2findClassTimeMap = new HashMap<>(8);
    /**
     *
     */
    private static final Map<String, Integer> canonicalText2TreeLevel = new HashMap<>(8);

    /**
     *
     */
    private static final Map<String, String> canonicalText2TInnerClassInterfaceContent = new HashMap<>(8);


    public static final String requireSplitTag = ": ";
    public static final String notRequireSplitTag = "?: ";

    /**
     *
     */
    public static void clearCache() {
        canonicalText2TreeLevel.clear();
        canonicalText2TInnerClassInterfaceContent.clear();
        canonicalText2findClassTimeMap.clear();
    }

    /**
     *
     * @param project
     * @param psiJavaFile
     * @return
     */
    public static String generatorInterfaceContentForPsiJavaFile(Project project, PsiJavaFile psiJavaFile) {
        String interfaceContent;
        try {
            interfaceContent = generatorInterfaceContentForPsiJavaFile(project, psiJavaFile, true, 1);
            StringBuilder stringBuilder = new StringBuilder();
            // to list
            List<Map.Entry<String, Integer>> list = new ArrayList<>(canonicalText2TreeLevel.entrySet());
            list.sort(Comparator.comparingInt(Map.Entry::getValue));

            Map<String, Integer> map2 = new LinkedHashMap<>();  // LinkedHashMap to order
            for (Map.Entry<String, Integer> o : list) {
                map2.put(o.getKey(), o.getValue());
            }

            stringBuilder.append(interfaceContent).append("\n");
            for (Map.Entry<String, Integer> entry : map2.entrySet()) {  // out
                String key = entry.getKey();
                String content = canonicalText2TInnerClassInterfaceContent.getOrDefault(key,"");
                if (content != null) {
                    stringBuilder.append(content).append("\n");
                }
            }

            return stringBuilder.toString();
        } finally {
            clearCache();
        }



    }

    /**
     *
     * @param project
     * @param psiJavaFile
     * @param isDefault
     * @param treeLevel
     * @return
     */
    public static String generatorTypeContent(Project project, PsiJavaFile psiJavaFile, boolean isDefault, int treeLevel) {
        StringBuilder typeContent = new StringBuilder();
        String defaultText = "";
        if (isDefault) {
            defaultText = "default ";
        }
        PsiClass[] classes = psiJavaFile.getClasses();
        for (PsiClass aClass : classes) {
            String classNameAsTypeName = aClass.getName();
            typeContent.append("export ").append(defaultText).append("type ").append(classNameAsTypeName).append(" = ");
            PsiField[] allFields = aClass.getAllFields();
            List<String> enumConstantValueList = new ArrayList<>();
            enumConstantValueList.add("string");
            for (PsiField psiField : allFields) {
                if (psiField instanceof PsiEnumConstant) {
                    String name = psiField.getName();
                    // as string
                    String value = "'" + name + "'";
                    enumConstantValueList.add(value);
                }
            }
            String join = String.join(" | ", enumConstantValueList);
            typeContent.append(join);
        }
        return typeContent + "\n";
    }

    /**
     *
     * @param project
     * @param psiJavaFile
     * @param isDefault
     * @param treeLevel
     * @return
     */
    public static String generatorInterfaceContentForPsiJavaFile(Project project, PsiJavaFile psiJavaFile,
                                                                 boolean isDefault, int treeLevel) {
        StringBuilder interfaceContent = new StringBuilder();
        String defaultText = "";
        if (isDefault) {
            defaultText = "default ";
        }
        PsiClass[] classes = psiJavaFile.getClasses();
        for (PsiClass aClass : classes) {
            doClassInterfaceContentForTypeScript(project, treeLevel, interfaceContent, defaultText, aClass);
        }
        return interfaceContent.toString();
    }

    /**
     *
     * @param project
     * @param psiClassInParameters
     * @param isDefault
     * @param treeLevel
     * @return
     */
    public static String generatorInterfaceContentForPsiClass(Project project,
                                                              PsiClass psiClassInParameters, PsiClass targetPsiClass,
                                                              boolean isDefault, int treeLevel) {
        StringBuilder interfaceContent = new StringBuilder();
        String defaultText = "";
        if (isDefault) {
            defaultText = "default ";
        }

        PsiClass innerClassByName = psiClassInParameters.findInnerClassByName(targetPsiClass.getName(), true);
        doClassInterfaceContentForTypeScript(project, treeLevel, interfaceContent, defaultText, innerClassByName);
        return interfaceContent.toString();
    }

    /**
     *
     *
     * @param project
     * @param treeLevel
     * @param interfaceContent
     * @param defaultText
     * @param aClass
     */
    private static void doClassInterfaceContentForTypeScript(Project project, int treeLevel,
                                                             StringBuilder interfaceContent,
                                                             String defaultText, PsiClass aClass) {
        canonicalText2findClassTimeMap.put(aClass.getQualifiedName(), 2);
        if (aClass.getQualifiedName() != null && aClass.getQualifiedName().startsWith("java.")) {
            return;
        }
        String classNameAsInterfaceName = aClass.getName();
        interfaceContent.append("export ").append(defaultText).append("interface ").append(classNameAsInterfaceName).append(" {\n");
        PsiField[] allFields = aClass.getAllFields();
        for (int i = 0; i < allFields.length; i++) {
            PsiField fieldItem = allFields[i];
            String fieldSplitTag = notRequireSplitTag;
            PsiAnnotation[] annotations = fieldItem.getAnnotations();
            boolean fieldRequire = CommonUtils.isFieldRequire(annotations);
            if (fieldRequire) {
                fieldSplitTag = requireSplitTag;
            }
            // 获取注释
            PsiDocComment docComment = fieldItem.getDocComment();
            if (docComment != null) {
                String docCommentText = docComment.getText();
                if (docCommentText != null) {
                    String[] split = docCommentText.split("\\n");
                    for (String docCommentLine : split) {
                        docCommentLine = docCommentLine.trim();
                        interfaceContent.append("  ").append(docCommentLine).append("\n");
                    }
                }
            }

            String name = fieldItem.getName();
            interfaceContent.append("  ").append(name);
            boolean isArray = CommonUtils.isArray(fieldItem);
            if (isArray) {
                // get generics
                String generics = CommonUtils.getGenericsForArray(fieldItem);
                interfaceContent.append(fieldSplitTag).append(generics);

                if (!CommonUtils.isTypescriptPrimaryType(generics)) {
                    String canonicalText = CommonUtils.getJavaBeanTypeForArrayField(fieldItem);
                    GlobalSearchScope projectScope = GlobalSearchScope.allScope(project);
                    Integer findClassTime = canonicalText2findClassTimeMap.getOrDefault(canonicalText, 0);
                    if (findClassTime == 0) {
                        canonicalText2findClassTimeMap.put(canonicalText, 1);
                        // todo
                        PsiClass psiClass = getPsiClass(project, canonicalText, projectScope);
                        if (psiClass != null) {
                            PsiElement parent = psiClass.getParent();
                            if (parent instanceof PsiJavaFile) {
                                PsiJavaFile classParentJavaFile = (PsiJavaFile) parent;
                                String findClassContent = generatorInterfaceContentForPsiJavaFile(project, classParentJavaFile,
                                        false, treeLevel + 1);
                                canonicalText2TInnerClassInterfaceContent.put(canonicalText, findClassContent);
                            } else if (parent instanceof PsiClass) {
                                PsiClass psiClassParent = (PsiClass) parent;
                                String findClassContent = generatorInterfaceContentForPsiClass(project, psiClassParent, psiClass, false, treeLevel + 1);
                                canonicalText2TInnerClassInterfaceContent.put(canonicalText, findClassContent);
                            }
                        }
                    }
                    canonicalText2TreeLevel.put(canonicalText, treeLevel);
                }
            } else if (CommonUtils.isNumber(fieldItem)) {
                interfaceContent.append(fieldSplitTag).append("number");
            } else if (CommonUtils.isString(fieldItem)) {
                interfaceContent.append(fieldSplitTag).append("string");
            } else if (CommonUtils.isBoolean(fieldItem)) {
                interfaceContent.append(fieldSplitTag).append("boolean");
            } else {
                String canonicalText = CommonUtils.getJavaBeanTypeForNormalField(fieldItem);
                GlobalSearchScope projectScope = GlobalSearchScope.allScope(project);

                Integer findClassTime = canonicalText2findClassTimeMap.getOrDefault(canonicalText, 0);
                if (findClassTime == 0) {
                    canonicalText2findClassTimeMap.put(canonicalText, 1);
                    PsiClass psiClass = getPsiClass(project, canonicalText, projectScope);
                    if (psiClass != null) {
                        JvmClassKind classKind = psiClass.getClassKind();
                        //  2022-08-09  ignroe ANNOTATION and  INTERFACE
                        if (classKind != JvmClassKind.ANNOTATION && classKind != JvmClassKind.INTERFACE) {
                            JvmReferenceType superClassType = psiClass.getSuperClassType();
                            if (superClassType != null && "Enum".equalsIgnoreCase(superClassType.getName())) {
                                // Enum
                                PsiElement parent = psiClass.getParent();
                                if (parent instanceof PsiJavaFile) {
                                    PsiJavaFile enumParentJavaFile = (PsiJavaFile) parent;
                                    String findTypeContent = generatorTypeContent(project, enumParentJavaFile, false, treeLevel);
                                    canonicalText2TInnerClassInterfaceContent.put(canonicalText, findTypeContent);
                                }
                            } else {
                                // class
                                PsiElement parent = psiClass.getParent();
                                // 内部类parent instanceof PsiJavaFile  ==false
                                if (parent instanceof PsiJavaFile) {
                                    PsiJavaFile classParentJavaFile = (PsiJavaFile) parent;
                                    String findClassContent = generatorInterfaceContentForPsiJavaFile(project, classParentJavaFile, false, treeLevel + 1);
                                    canonicalText2TInnerClassInterfaceContent.put(canonicalText, findClassContent);
                                } else if (parent instanceof PsiClass) {
                                    PsiClass psiClassParent = (PsiClass) parent;
                                    String findClassContent = generatorInterfaceContentForPsiClass(project, psiClassParent, psiClass, false, treeLevel + 1);
                                    canonicalText2TInnerClassInterfaceContent.put(canonicalText, findClassContent);
                                }
                            }
                        }

                    }
                }
                canonicalText2TreeLevel.put(canonicalText, treeLevel);

                if (canonicalText2TInnerClassInterfaceContent.get(canonicalText) != null
                        || canonicalText2findClassTimeMap.getOrDefault(canonicalText, 0) == 2) {
                    String shortName = StringUtil.getShortName(canonicalText);
                    interfaceContent.append(fieldSplitTag).append(shortName);
                } else {
                    interfaceContent.append(fieldSplitTag).append("any");
                }
            }

            // end of field
            if (isArray) {
                interfaceContent.append("[]");
            }
            if (i != allFields.length - 1) {
                interfaceContent.append("\n");
            }
            // end of filed
            interfaceContent.append("\n");
        }
        // end of class
        interfaceContent.append("}\n");
    }

    private static PsiClass getPsiClass(Project project, String canonicalText,
                                        GlobalSearchScope projectScope) {
        PsiClass[] psiClasses = JavaPsiFacade.getInstance(project).findClasses(canonicalText, projectScope);
        if (psiClasses.length == 0) {
            return null;
        }
        for (PsiClass psiClass : psiClasses) {
            if (psiClass instanceof PsiJavaFile) {
                return psiClass;
            }
        }
        PsiClass psiClass = psiClasses[0];
        if (psiClass instanceof ClsClassImpl) {
            ClsClassImpl clsClassImpl = (ClsClassImpl) psiClass;
            PsiClass sourceMirrorClass = clsClassImpl.getSourceMirrorClass();
            if (sourceMirrorClass != null) {
                return sourceMirrorClass;
            }
        }
        return psiClass;
    }


}
