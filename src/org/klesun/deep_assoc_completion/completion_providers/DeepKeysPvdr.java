package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.intellij.ui.JBColor;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayAccessExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import java.awt.*;
import java.util.*;

import static org.klesun.lang.Lang.L;
import static org.klesun.lang.Lang.toCast;

/**
 * contains the completion logic
 */
public class DeepKeysPvdr extends CompletionProvider<CompletionParameters>
{
    final private static int BRIEF_TYPE_MAX_LEN = 45;
    final private static Color KEY_NAME_COLOR = new Color(60, 120, 40);

    private static LookupElement makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(PhpIcons.FIELD)
            .withTypeText(type);
    }

    /**
     * unlike built-in LookupElement, this one can be changed after being
     * displayed (if more detailed type info was calculated in background)
     */
    static class MutableLookup extends LookupElement
    {
        public LookupElement lookupData;
        public MutableLookup(LookupElement lookupData)
        {
            this.lookupData = lookupData;
        }
        @NotNull public String getLookupString() {
            return lookupData.getLookupString();
        }
        public void renderElement(LookupElementPresentation presentation) {
            lookupData.renderElement(presentation);
        }
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        SearchContext search = new SearchContext().setDepth(35);
        IFuncCtx funcCtx = new FuncCtx(search, L());

        Set<String> suggested = new HashSet<>();

        long startTime = System.nanoTime();
        Lang.opt(parameters.getPosition().getParent())
            .thn(literal -> Lang.opt(literal.getParent())
                .fop(Lang.toCast(ArrayIndex.class))
                .map(index -> index.getParent())
                .fop(Lang.toCast(ArrayAccessExpressionImpl.class))
                .map(expr -> expr.getValue())
                .fop(toCast(PhpExpression.class))
                .map(srcExpr -> funcCtx.findExprType(srcExpr).types)
                .thn(types -> {
                    MultiType mt = new MultiType(types);
                    L<String> keyNames = mt.getKeyNames();
                    L<DeepType> indexTypes = mt.types.fap(t -> t.indexTypes);

                    if (indexTypes.size() > 0) {
                        String typeText = new MultiType(L(indexTypes)).getBriefTypeText(BRIEF_TYPE_MAX_LEN);
                        for (int k = 0; k < 5; ++k) {
                            result.addElement(makeLookupBase(k + "", typeText));
                        }
                    }

                    L<MutableLookup> lookups = keyNames
                        .map(keyName -> {
                            String briefTypeRaw = mt.getKeyBriefType(keyName).filterUnknown().toStringResolved();
                            briefTypeRaw = !briefTypeRaw.equals("") ? briefTypeRaw : "?";
                            // to define dialog pop-up width
                            briefTypeRaw = "                                                                  " + briefTypeRaw;
                            briefTypeRaw = Tls.substr(briefTypeRaw, -BRIEF_TYPE_MAX_LEN);
                            String briefType = briefTypeRaw;
                            LookupElement lookup =  LookupElementBuilder.create(keyName)
                                .bold()
                                .withIcon(PhpIcons.FIELD)
                                .withTypeText(briefType, true);
                            return new MutableLookup(lookup);
                        });

                    suggested.addAll(lookups.map(l -> l.getLookupString()));

                    // preliminary keys without type - they may be at least 3 times faster in some cases
                    result.addAllElements(lookups.map((lookup, i) ->
                        PrioritizedLookupElement.withPriority(lookup, 2000 - i)));

                    Lang.Dict<LookupElement> nameToNewLookup = keyNames
                        .key(keyName -> keyName)
                        .map(keyName -> makeLookupBase(keyName, mt.getKey(keyName).getBriefTypeText(BRIEF_TYPE_MAX_LEN)));

                    lookups.fch(l -> nameToNewLookup.gat(l.getLookupString()).thn(newL -> l.lookupData = newL));

                    long elapsed = System.nanoTime() - startTime;
                    result.addLookupAdvertisement("Resolved " + search.getExpressionsResolved() + " expressions in " + (elapsed / 1000000000.0) + " seconds");
                })
                .els(() -> result.addLookupAdvertisement("Failed to find declared array keys")));

        result.runRemainingContributors(parameters, otherSourceResult -> {
            // remove dupe built-in suggestions
            LookupElement lookup = otherSourceResult.getLookupElement();
            if (!suggested.contains(lookup.getLookupString())) {
                result.addElement(lookup);
            }
        });

        long elapsed = System.nanoTime() - startTime;
        if (search.getExpressionsResolved() > 0) {
            System.out.println("Resolved " + search.getExpressionsResolved() + " expressions in " + (elapsed / 1000000000.0) + " seconds");
        }
    }
}
