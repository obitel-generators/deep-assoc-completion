package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpCaches;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.MemberReference;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

import java.util.Map;

/**
 * for cases when built-in Type Provider failed to determine
 * class of object - try ourselves using the Deep Resolver
 */
public class DeepObjMemberGoToDecl extends Lang implements GotoDeclarationHandler
{
    private L<? extends PsiElement> resolveMember(PhpClass cls, String name)
    {
        return list(
            L(cls.getFields()).flt(f -> f.getName().equals(name)),
            L(cls.getMethods()).flt(m -> m.getName().equals(name))
        ).fap(a -> a);
    }

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor)
    {
        SearchContext search = new SearchContext()
            .setDepth(DeepKeysPvdr.getMaxDepth(false));
        FuncCtx funcCtx = new FuncCtx(search);

        L<? extends PsiElement> psiTargets = opt(psiElement)
            .map(leaf -> leaf.getParent())
            .fop(toCast(MemberReference.class))
            .flt(mem -> opt(mem.getClassReference())
                // skip this provider if IDEA already resolved the class
                .flt(ref -> {
                    PhpType clean = ref.getType().filterUnknown().filterUnknown().filterNull().filterNull().filterMixed().filter(PhpType.OBJECT);
                    return clean.isEmpty() || clean.toString().equals("\\stdClass");
                })
                .has())
            .fap(mem -> opt(mem.getFirstChild())
                .fop(toCast(PhpExpression.class))
                .map(exp -> funcCtx.findExprType(exp))
                .fap(mt -> list(
                    ArrCtorRes.resolveMtCls(mt, mem.getProject())
                        .fap(cls -> resolveMember(cls, mem.getName())),
                    mt.getProps()
                        .flt(prop -> prop.name.equals(mem.getName()))
                        .map(prop -> prop.definition)
                ).fap(a -> a))
            );

        return psiTargets.toArray(new PsiElement[psiTargets.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext)
    {
        return null;
    }
}
