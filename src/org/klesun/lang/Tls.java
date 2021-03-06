package org.klesun.lang;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * provides static functions that would
 * be useful in any kind of situation
 */
public class Tls extends Lang
{
    /**
     * unlike built-in SomeClass.class.cast(),
     * this returns empty optional instead of
     * throwing exception if cast fails
     */
    public static <T extends U, U> Opt<T> cast(Class<T> cls, U value)
    {
        if (cls.isInstance(value)) {
            return new Opt<>(cls.cast(value));
        } else {
            return new Opt<>(null);
        }
    }

    public static String getStackTrace(Throwable exc)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exc.printStackTrace(pw);
        return sw.toString();
    }

    public static String getStackTrace()
    {
        return getStackTrace(new Exception());
    }

    public static It<PsiElement> getNextSiblings(PsiElement psi)
    {
        PsiElement next = psi.getNextSibling();
        return It(() -> new Iterator<PsiElement>() {
            private PsiElement current = next;
            public boolean hasNext() {
                return current != null;
            }
            public PsiElement next() {
                PsiElement prev = current;
                current = current.getNextSibling();
                return prev;
            }
        });
    }

    public static It<PsiElement> getParents(PsiElement psi)
    {
        PsiElement parent = psi.getParent();
        return It(() -> new Iterator<PsiElement>() {
            private PsiElement current = parent;
            public boolean hasNext() {
                return current != null;
            }
            public PsiElement next() {
                PsiElement prev = current;
                current = current.getParent();
                return prev;
            }
        });
    }

    public static <T extends PsiElement> Opt<T> findParent(
        PsiElement psi,
        Class<T> cls,
        Predicate<PsiElement> continuePred
    ) {
        for (PsiElement parent: getParents(psi)) {
            Opt<T> matching = Tls.cast(cls, parent);
            if (matching.has()) {
                return matching;
            } else if (!continuePred.test(parent)) {
                break;
            }
        }
        return opt(null);
    }

    public static <T extends PsiElement> Opt<T> findParent(
        PsiElement psi,
        Class<T> cls
    ) {
        return findParent(psi, cls, a -> true);
    }

    public static <T extends PsiElement> Opt<T> findPrevSibling(PsiElement psi, Class<T> cls)
    {
        PsiElement prev = psi.getPrevSibling();
        while (prev != null) {
            Opt<T> matching = Tls.cast(cls, prev);
            if (matching.has()) {
                return matching;
            }
            prev = prev.getPrevSibling();
        }
        return non();
    }

    public static It<PsiElement> getChildrenWithLeaf(PsiElement psi)
    {
        PsiElement next = psi.getFirstChild();
        return It(() -> new Iterator<PsiElement>() {
            private PsiElement nextProp = next;
            public boolean hasNext() {
                return nextProp != null;
            }
            public PsiElement next() {
                PsiElement current = nextProp;
                nextProp = nextProp.getNextSibling();
                return current;
            }
        });
    }

    public static <T extends PsiElement> It<T> findChildren(
        PsiElement parent,
        Class<T> cls,
        Predicate<PsiElement> goDeeperPred
    ) {
        if (goDeeperPred.test(parent)) {
            return It.cnc(
                Tls.cast(cls, parent),
                It(parent.getChildren())
                    .fap(c -> findChildren(c, cls, goDeeperPred))
            );
        } else {
            return It.non();
        }
    }

    public static <T extends PsiElement> It<T> findChildren(
        PsiElement parent,
        Class<T> cls
    ) {
        return findChildren(parent, cls, (a) -> true);
    }

    public static Opt<L<String>> regexWithFull(String patternText, String subjectText, int flags)
    {
        List<String> result = list();
        Pattern pattern = Pattern.compile(patternText, flags);
        Matcher matcher = pattern.matcher(subjectText);
        if (matcher.matches()) {
            for (int i = 0; i < matcher.groupCount() + 1; ++i) {
                result.add(matcher.group(i));
            }
            return opt(L(result));
        } else {
            return opt(null);
        }
    }

    /**
     * be careful, java's regex implementation matches WHOLE string, in other
     * words, it implicitly adds "^" and "$" at beginning and end of your regex
     *
     * be careful, this function does not return _whole_ match, so 0-th returned group will contain the
     * first () group from your regex (which is usually the 1-th returned group in return in php/js/python/etc...)
     */
    public static Opt<L<String>> regex(String patternText, String subjectText, int flags)
    {
        return regexWithFull(patternText, subjectText, flags)
            .map(matches -> matches.sub(1));
    }

    public static Opt<L<String>> regex(String patternText, String subjectText)
    {
        return regex(patternText, subjectText, Pattern.DOTALL);
    }

    /** make object string representation _approximately_ resembling json for debug */
    public static String json(Object value)
    {
        if (value instanceof List) {
            List list = (List)value;
            String result = "[";
            for (int i = 0; i < list.size(); ++i) {
                result += json(list.get(i));
                if (i < list.size() - 1) {
                    result += ",";
                }
            }
            return result + "]";
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else {
            return value.toString();
        }
    }

    public static String implode(String delimiter, Iterable<String> valueIt)
    {
        L<String> values = L(valueIt);
        String result = "";
        for (int i = 0; i < values.size(); ++i) {
            result += values.get(i);
            if (i < values.size() - 1) {
                result += delimiter;
            }
        }
        return result;
    }

    public static L<String> diff(L<String> a, L<String> b)
    {
        L<String> result = list();
        Set<String> bSet = new HashSet<>(b);
        for (String value: a) {
            if (!bSet.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    public interface IOnDemand<T> extends S<T>
    {
        Opt<T> ifHas();
    }

    public static class OnDemand<T> implements IOnDemand<T>
    {
        final private S<T> f;
        private Opt<T> value = non();

        OnDemand(S<T> f)
        {
            this.f = f;
            if (f instanceof Granted) {
                this.get();
            }
        }

        @Override
        public T get() {
            if (!value.has()) {
                value = new Opt<>(f.get(), true);
            }
            return value.unw();
        }
        public boolean has() {
            return value.has();
        }

        public Opt<T> ifHas() {
            return value;
        }
    }

    public static <T> OnDemand<T> onDemand(S<T> f)
    {
        return new OnDemand<>(f);
    }

    public static It<Integer> range(int l, int r)
    {
        return new It<>(IntStream.range(l, r).boxed().collect(Collectors.toList()));
    }

    /**
     * removes line breaks from string - useful sometimes
     * if you want to preserve formatting in debug
     */
    public static String singleLine(String text, int length)
    {
        It<String> lines = It(text.split("\n")).map(String::trim);
        return Tls.substr(Tls.implode(" ", lines), 0, length);
    }

    public static String singleLine(String text)
    {
        return singleLine(text, 60);
    }

    public static boolean isNum(String str)
    {
        return regex("^\\d+$", str).has();
    }

    /**
     * get built-in type, refresh cache if empty
     * (with advanced type resolver this time)
     */
    public static PhpType getIdeaType(PhpExpression exp)
    {
        PhpType type = exp.getType();
        return type;
    }

    public static <T> T ife(boolean cond, S<T> then, S<T> els)
    {
        if (cond) {
            return then.get();
        } else {
            return els.get();
        }
    }

    public static <T> It<T> ifi(boolean cond, S<Iterable<T>> then)
    {
        if (cond) {
            return It(then.get());
        } else {
            return It(list());
        }
    }
}
