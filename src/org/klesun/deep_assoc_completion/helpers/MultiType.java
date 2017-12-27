package org.klesun.deep_assoc_completion.helpers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.lang.Lang;
import org.klesun.lang.NonNull;
import org.klesun.lang.Tls;

import java.util.HashSet;

/**
 * this data structure represents a list of
 * DeepTypes-s that some variable mya have
 * it's more readable type annotation than L<DeepType>
 *
 * it also probably could give some handy methods
 * like getKey(), elToArr(), arToEl() - all the
 * static functions that take list of typeGetters
 */
public class MultiType extends Lang
{
    static enum REASON {OK, CIRCULAR_REFERENCE, FAILED_TO_RESOLVE, DEPTH_LIMIT, INVALID_PSI}
    public static MultiType CIRCULAR_REFERENCE = new MultiType(L(), REASON.CIRCULAR_REFERENCE);
    public static MultiType INVALID_PSI = new MultiType(L(), REASON.INVALID_PSI);

    private REASON reason;
    final public L<DeepType> types;

    public MultiType(L<DeepType> types, REASON reason)
    {
        this.types = types;
        this.reason = reason;
    }
    public MultiType(L<DeepType> types)
    {
        this(types, REASON.OK);
    }

    public MultiType getEl()
    {
        return new MultiType(types.fap(arrt -> {
            L<DeepType> mixed = L();
            mixed.addAll(arrt.indexTypes);
            arrt.keys.forEach((k,v) -> mixed.addAll(v.getTypes()));
            return mixed;
        }));
    }

    public DeepType getInArray(PsiElement call)
    {
        DeepType result = new DeepType(call, PhpType.ARRAY);
        result.indexTypes.addAll(types);
        return result;
    }

    public String getStringValue()
    {
        if (types.size() == 1) {
            return types.get(0).stringValue;
        } else {
            return null;
        }
    }

    public MultiType getKey(String keyName)
    {
        return new MultiType(list(
            // if keyName is a constant string - return type of this key only
            types.flt(t -> keyName != null)
                .fop(type -> Lang.getKey(type.keys, keyName))
                .fap(v -> v.getTypes()),
            // if keyName is a var - return types of all keys
            types.flt(t -> keyName == null)
                .fap(t -> L(t.keys.values()))
                .fap(v -> v.getTypes()),
            types.fap(t -> t.indexTypes)
        ).fap(a -> a));
    }

    public PhpType getKeyBriefType(@NonNull String keyName)
    {
        PhpType ideaType = new PhpType();
        types.fop(t -> Lang.getKey(t.keys, keyName))
            .fap(k -> k.getBriefTypes())
            .fch(ideaType::add);
        return ideaType;
    }

    public L<String> getKeyNames()
    {
        L<String> names = L();
        HashSet<String> repeations = new HashSet<>();
        types.fap(t -> L(t.keys.keySet())).fch(name -> {
            if (!repeations.contains(name)) {
                repeations.add(name);
                names.add(name);
            }
        });
        return names;
    }

    public PhpType getIdeaType()
    {
        PhpType ideaType = new PhpType();
        types.map(t -> t.briefType).fch(ideaType::add);
        return ideaType;
    }

    public String getBriefTypeText(int maxLen)
    {
        L<String> briefTypes = list();
        L<String> keyNames = getKeyNames();

        if (keyNames.size() > 0) {
            if (keyNames.all((k,i) -> (k + "").equals(i + ""))) {
                briefTypes.add("(" + Tls.implode(", ", keyNames.map(i -> getKey(i + "").getBriefTypeText(15))) + ")");
            } else {
                briefTypes.add("{" + Tls.implode(", ", keyNames.map(k -> k + ":")) + "}");
            }
        }
        L<String> strvals = types.fop(t -> opt(t.stringValue));
        if (strvals.size() > 0) {
            briefTypes.add(Tls.implode("|", strvals
                .grp(a -> a).fop(g -> g.fst())
                .map(s -> "'" + s + "'")));
        }
        if (types.any(t -> t.indexTypes.size() > 0)) {
            briefTypes.add("[" + getEl().getBriefTypeText() + "]");
        }
        if (briefTypes.isEmpty() || briefTypes.all((t,i) -> t.equals(""))) {
            briefTypes.add(getIdeaType().filterUnknown().toString());
        }
        String fullStr = Tls.implode("|", briefTypes);
        fullStr = fullStr.length() == 0 ? "?" : fullStr;
        String truncated = Tls.substr(fullStr, 0, maxLen);
        return truncated.length() == fullStr.length()
            ? truncated : truncated + "...";
    }

    public String getBriefTypeText()
    {
        return getBriefTypeText(50);
    }

    public String toJson()
    {
        return DeepType.toJson(types, 0);
    }
}