package org.klesun.deep_assoc_completion.structures;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.helpers.Mt;
import static org.klesun.lang.Lang.*;

/** short for "Make Type" */
public class Mkt {
    public static DeepType str(PsiElement psi, String content)
    {
        return new DeepType(psi, PhpType.STRING, content);
    }

    public static DeepType str(PsiElement psi)
    {
        return new DeepType(psi, PhpType.STRING);
    }

    public static DeepType inte(PsiElement psi)
    {
        return new DeepType(psi, PhpType.INT);
    }

    public static DeepType inte(PsiElement psi, Integer content)
    {
        return new DeepType(psi, PhpType.INT, content + "");
    }

    public static DeepType floate(PsiElement psi)
    {
        return new DeepType(psi, PhpType.FLOAT);
    }

    public static DeepType floate(PsiElement psi, double content)
    {
        return new DeepType(psi, PhpType.FLOAT, content + "");
    }

    public static DeepType bool(PsiElement psi)
    {
        return new DeepType(psi, PhpType.BOOLEAN);
    }

    public static DeepType bool(PsiElement psi, Boolean content)
    {
        return new DeepType(psi, PhpType.BOOLEAN, (content ? 1 : 0) + "");
    }

    public static DeepType arr(PsiElement psi)
    {
        return new DeepType(psi, PhpType.ARRAY);
    }

    public static DeepType arr(PsiElement psi, Mt elt)
    {
        return elt.getInArray(psi);
    }

    public static DeepType assoc(PsiElement psi, Iterable<T2<String, Mt>> keys)
    {
        DeepType assoct = new DeepType(psi, PhpType.ARRAY, false);
        for (T2<String, Mt> key: keys) {
            PhpType ideaType = key.b.getIdeaTypes().fst().def(PhpType.UNSET);
            assoct.addKey(key.a, psi).addType(Granted(key.b), ideaType);
        }
        return assoct;
    }

    public static DeepType mixed(PsiElement psi)
    {
        return new DeepType(psi, PhpType.MIXED);
    }
}