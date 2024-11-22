package com.lxy.sean.j2ts.utils;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.PsiAnnotationImpl;

import java.util.Arrays;
import java.util.List;

public class CommonUtils {

    public static final List<String> numberTypes = Arrays.asList("byte", "short", "int", "long", "double", "float");

    public static final List<String> requireAnnotationShortNameList = Arrays.asList("NotNull", "NotEmpty", "NotBlank");


    /**
     * Determine whether the field is a numeric type
     *
     * @param field
     * @return
     */
    public static boolean isNumber(PsiField field) {
        if (Arrays.stream(field.getType().getSuperTypes())
                .anyMatch(superTypeItem -> superTypeItem.getCanonicalText().equals("java.lang.Number"))) {
            return true;
        }
        String canonicalText = field.getType().getCanonicalText();
        return numberTypes.contains(canonicalText);
    }

    /**
     *
     * @param field
     * @return
     */
    public static String getGenericsForArray(PsiField field) {
        if (!isArray(field)) {
            throw new RuntimeException("target field is not  array type");
        }
        PsiType type = field.getType();

        if (type instanceof PsiArrayType) {
            PsiArrayType psiArrayType = (PsiArrayType) type;
            // 数组
            PsiType deepComponentType = psiArrayType.getDeepComponentType();
            if (Arrays.stream(deepComponentType.getSuperTypes())
                    .anyMatch(superTypeItem -> superTypeItem.getCanonicalText().equals("java.lang.Number"))) {
                return "number";
            }
            String canonicalText = deepComponentType.getCanonicalText();
            if ("java.lang.Boolean".equals(canonicalText)) {
                return "boolean";
            } else if ("java.lang.String".equals(canonicalText)) {
                return "string";
            } else {
                return deepComponentType.getPresentableText();
            }
        } else if (type instanceof PsiClassReferenceType) {
            PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
            // 集合
            PsiType[] parameters = psiClassReferenceType.getParameters();
            if (parameters.length == 0) {
                return "any";
            }

            PsiType deepComponentType = parameters[0].getDeepComponentType();
            // 判断泛型是不是number
            if (Arrays.stream(deepComponentType.getSuperTypes())
                    .anyMatch(superTypeItem -> superTypeItem.getCanonicalText().equals("java.lang.Number"))) {
                return "number";
            }
            String canonicalText = deepComponentType.getCanonicalText();
            if ("java.lang.Boolean".equals(canonicalText)) {
                return "boolean";
            } else if ("java.lang.String".equals(canonicalText)) {
                return "string";
            } else {
                return deepComponentType.getPresentableText();
            }
        }

        return "any";
    }

    public static boolean isTypescriptPrimaryType(String type) {
        return "number".equals(type) || "string".equals(type) || "boolean".equals(type);
    }

    public static String getJavaBeanTypeForArrayField(PsiField field) {
        if (isArray(field)) {
            PsiType type = field.getType();

            if (type instanceof PsiArrayType) {
                PsiArrayType psiArrayType = (PsiArrayType) type;
                // 数组
                PsiType deepComponentType = psiArrayType.getDeepComponentType();
                return deepComponentType.getCanonicalText();
            } else if (type instanceof PsiClassReferenceType) {
                PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
                // 集合
                PsiType[] parameters = psiClassReferenceType.getParameters();
                PsiType deepComponentType = parameters[0].getDeepComponentType();
                return deepComponentType.getCanonicalText();
            } else {
                return "any";
            }
        } else {
            throw new RuntimeException("target field is not  array type");
        }
    }

    public static String getJavaBeanTypeForNormalField(PsiField field) {

            PsiType type = field.getType();

            if (type instanceof PsiArrayType) {
                PsiArrayType psiArrayType = (PsiArrayType) type;
                PsiType deepComponentType = psiArrayType.getDeepComponentType();
                return deepComponentType.getCanonicalText();
            } else if (type instanceof PsiClassReferenceType) {
                PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
                PsiType deepComponentType = psiClassReferenceType.getDeepComponentType();
                return deepComponentType.getCanonicalText();
            } else {
                return "any";
            }

    }

    /**
     * Check whether the field is an array
     *
     * @param field
     * @return
     */
    public static boolean isArray(PsiField field) {
        if (field.getText().contains("[]")) {
            return true;
        }
        return Arrays.stream(field.getType().getSuperTypes())
                .anyMatch(superType -> superType.getCanonicalText().contains("java.util.Collection<"));
    }

    public static boolean isString(PsiField field) {
        String presentableText = field.getType().getPresentableText();
        return presentableText.equals("String");
    }

    public static boolean isBoolean(PsiField field) {
        String canonicalText = field.getType().getCanonicalText();
        return "java.lang.Boolean".equals(canonicalText) || "boolean".equals(canonicalText);
    }

    /**
     * 判断字段是否是必须的
     *
     * @param annotations
     * @return
     */
    public static boolean isFieldRequire(PsiAnnotation[] annotations) {
        if (null == annotations) {
            return false;
        }
        for (PsiAnnotation annotation : annotations) {
            if (!(annotation instanceof PsiAnnotationImpl)) {
                continue;
            }
            PsiAnnotationImpl annotationImpl = (PsiAnnotationImpl) annotation;
            String qualifiedName = annotationImpl.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }
            String shortName = StringUtil.getShortName(qualifiedName);
            if (requireAnnotationShortNameList.stream()
                    .anyMatch(item -> item.equalsIgnoreCase(shortName))) {
                return true;
            }
        }
        return false;
    }


    /**
     *
     *
     * @param title       标题
     * @param description 描述
     * @return FileChooserDescriptor
     */
    public static FileChooserDescriptor createFileChooserDescriptor(String title, String description) {
        FileChooserDescriptor singleFolderDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        if (title != null) {
            singleFolderDescriptor.setTitle(title);
        }
        if (description != null) {
            singleFolderDescriptor.setDescription(description);
        }
        return singleFolderDescriptor;
    }
}
