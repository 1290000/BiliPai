package com.android.purebilibili.lint;

import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.SourceCodeScanner;
import com.android.tools.lint.detector.api.SourceSetType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtParenthesizedExpression;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtReturnExpression;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.UNamedExpression;
import org.jetbrains.uast.UParenthesizedExpression;
import org.jetbrains.uast.UPrefixExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.USimpleNameReferenceExpression;
import org.jetbrains.uast.UastPrefixOperator;

/** Resolved Compose APIs in the six migrated feature packages, not source-text patterns. */
public final class StyleDetector extends Detector implements SourceCodeScanner {
    private static final Implementation IMPLEMENTATION =
            new Implementation(StyleDetector.class, Scope.JAVA_FILE_SCOPE);
    private static final String FEATURE = "com.android.purebilibili.feature.";
    private static final Set<String> MIGRATED =
            Set.of("live", "home", "dynamic", "following", "list", "watchlater");
    private static final Set<String> NAMED_COLORS = Set.of(
            "Black", "White", "Red", "Blue", "Green", "Yellow", "Gray", "LightGray", "DarkGray");

    public static final Issue COLOR = issue("BiliPaiHardcodedColor", "Literal Compose color",
            "Use theme roles or an explicitly named Palette/Policy token instead of a numeric "
                    + "Compose Color or one of the fixed named colors. Color.Transparent is allowed.");
    public static final Issue SPACING = issue("BiliPaiHardcodedSpacing", "Literal Compose dimension",
            "A numeric literal converted with Compose dp is a hardcoded dimension. This rule does "
                    + "not infer whether the dimension is spacing, size, offset, or a corner radius. "
                    + "Use a named dimension token or an approved file exemption.");
    public static final Issue TYPOGRAPHY = issue("BiliPaiHardcodedTypography", "Literal Compose text unit",
            "Use theme typography or a named typography token instead of converting a numeric literal to sp.");
    public static final Issue SHAPE = issue("BiliPaiHardcodedShape", "Literal rounded corner dimension",
            "Use a named shape token instead of supplying a numeric dp literal to Compose RoundedCornerShape.");
    public static final Issue MOTION = issue("BiliPaiHardcodedMotion", "Literal animation parameter",
            "Use a named motion token instead of supplying a numeric literal to Compose tween or spring.");
    public static final Issue SURFACE = issue("BiliPaiHardcodedSurface", "Direct base theme surface read",
            "Use a named surface role instead of directly reading MaterialTheme.colorScheme.surface or background.");
    public static final List<Issue> ISSUES = List.of(COLOR, SPACING, TYPOGRAPHY, SHAPE, MOTION, SURFACE);

    private static Issue issue(String id, String title, String explanation) {
        return Issue.create(id, title, explanation, Category.CORRECTNESS, 6, Severity.ERROR, IMPLEMENTATION);
    }

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return List.of(UCallExpression.class, USimpleNameReferenceExpression.class);
    }

    @Override
    public UElementHandler createUastHandler(JavaContext context) {
        if (context.getSourceSetType() != SourceSetType.MAIN || context.getUastFile() == null
                || !migratedPackage(context.getUastFile().getPackageName())) {
            return null;
        }
        return new UElementHandler() {
            @Override
            public void visitCallExpression(UCallExpression node) {
                if (namedTokenInitializer(node)) return;
                PsiMethod method = node.resolve();
                if (method == null) return;
                String owner = owner(method);
                String name = unmangled(method.getName());
                if (("androidx.compose.ui.graphics.ColorKt".equals(owner) && "Color".equals(name))
                        || ("androidx.compose.ui.graphics.Color".equals(owner) && method.isConstructor())) {
                    if (hasLiteralArgument(node, false)) {
                        report(context, COLOR, node, "Numeric literal supplied to Compose Color; use a named color token.");
                    }
                } else if (("androidx.compose.foundation.shape.RoundedCornerShapeKt".equals(owner)
                        && "RoundedCornerShape".equals(name))
                        || ("androidx.compose.foundation.shape.RoundedCornerShape".equals(owner)
                        && method.isConstructor())) {
                    if (hasLiteralArgument(node, true)) {
                        report(context, SHAPE, node, "Numeric dp literal supplied to RoundedCornerShape; use a named shape token.");
                    }
                } else if ("androidx.compose.animation.core.AnimationSpecKt".equals(owner)
                        && ("tween".equals(name) || "spring".equals(name))
                        && hasLiteralArgument(node, false) && !namedMotionFactory(node)) {
                    report(context, MOTION, node, "Numeric literal supplied to Compose " + name + "; use a named motion token.");
                }
            }

            @Override
            public void visitSimpleNameReferenceExpression(USimpleNameReferenceExpression node) {
                if (namedTokenInitializer(node)) return;
                PsiElement source = node.getSourcePsi();
                if (source != null
                        && PsiTreeUtil.getParentOfType(source, KtImportDirective.class, false) != null) return;
                PsiElement resolved = resolvedPsi(node.resolve());
                String name = propertyName(resolved);
                String owner = owner(resolved);
                UElement parent = node.getUastParent();
                if (parent instanceof UQualifiedReferenceExpression qualified
                        && Objects.equals(qualified.getSelector(), node)) {
                    if ("dp".equals(name) && "androidx.compose.ui.unit.DpKt".equals(owner)
                            && numericLiteral(qualified.getReceiver())) {
                        report(context, SPACING, qualified,
                                "Numeric literal converted to Compose dp; use a named dimension token (not necessarily layout spacing).");
                    } else if ("sp".equals(name) && "androidx.compose.ui.unit.TextUnitKt".equals(owner)
                            && numericLiteral(qualified.getReceiver())) {
                        report(context, TYPOGRAPHY, qualified,
                                "Numeric literal converted to Compose sp; use theme typography or a named text-unit token.");
                    } else if (("surface".equals(name) || "background".equals(name))
                            && "androidx.compose.material3.ColorScheme".equals(owner)
                            && materialColorScheme(qualified.getReceiver())) {
                        report(context, SURFACE, qualified,
                                "Direct MaterialTheme.colorScheme." + name + " read; use a named surface role.");
                    }
                }
                if (NAMED_COLORS.contains(name)
                        && ("androidx.compose.ui.graphics.Color.Companion".equals(owner)
                        || "androidx.compose.ui.graphics.Color".equals(owner))) {
                    report(context, COLOR, node, "Fixed Compose Color." + name + " read; use a named color token.");
                }
            }
        };
    }

    private static void report(JavaContext context, Issue issue, UElement node, String message) {
        context.report(issue, node, context.getLocation(node), message);
    }

    private static boolean migratedPackage(String packageName) {
        if (!packageName.startsWith(FEATURE)) return false;
        int start = FEATURE.length();
        int end = packageName.indexOf('.', start);
        return MIGRATED.contains(packageName.substring(start, end < 0 ? packageName.length() : end));
    }

    private static PsiElement resolvedPsi(PsiElement element) {
        if (element == null || element instanceof PsiMember) return element;
        PsiElement declaration = element.getNavigationElement();
        // Constructor properties can resolve to a light value parameter. Read the resolved
        // Kotlin declaration directly: value-class getter lookup can be absent or mangled.
        if (declaration instanceof KtParameter parameter && parameter.hasValOrVar()
                || declaration instanceof KtProperty) {
            return declaration;
        }
        return element;
    }

    private static String owner(PsiElement element) {
        if (element instanceof PsiMember member) {
            PsiClass containingClass = member.getContainingClass();
            return containingClass == null ? "" : containingClass.getQualifiedName();
        }
        if (element instanceof KtParameter || element instanceof KtProperty) {
            KtClassOrObject containingClass = PsiTreeUtil.getParentOfType(
                    element, KtClassOrObject.class, false);
            return containingClass == null || containingClass.getFqName() == null
                    ? "" : containingClass.getFqName().asString();
        }
        return "";
    }

    private static String unmangled(String name) {
        int separator = name.indexOf('-');
        return separator < 0 ? name : name.substring(0, separator);
    }

    private static String propertyName(PsiElement element) {
        if (element instanceof KtParameter parameter) return Objects.toString(parameter.getName(), "");
        if (element instanceof KtProperty property) return Objects.toString(property.getName(), "");
        if (!(element instanceof PsiMember member)) return "";
        String name = member.getName();
        if (name == null) return "";
        name = unmangled(name);
        if (element instanceof PsiMethod && name.startsWith("get") && name.length() > 3) {
            String property = name.substring(3);
            // Color's named properties deliberately start with an uppercase letter.
            if (NAMED_COLORS.contains(property)) return property;
            return Character.toLowerCase(property.charAt(0)) + property.substring(1);
        }
        return name;
    }

    private static UExpression unwrap(UExpression expression) {
        while (true) {
            if (expression instanceof UNamedExpression named) expression = named.getExpression();
            else if (expression instanceof UParenthesizedExpression parentheses) expression = parentheses.getExpression();
            else return expression;
        }
    }

    private static boolean numericLiteral(UExpression expression) {
        expression = unwrap(expression);
        if (expression instanceof UPrefixExpression prefix
                && (prefix.getOperator() == UastPrefixOperator.UNARY_MINUS
                || prefix.getOperator() == UastPrefixOperator.UNARY_PLUS)) {
            expression = unwrap(prefix.getOperand());
        }
        return expression instanceof ULiteralExpression literal && literal.getValue() instanceof Number;
    }

    private static boolean hasLiteralArgument(UCallExpression call, boolean dp) {
        for (UExpression argument : call.getValueArguments()) {
            if (dp ? literalDp(argument) : numericLiteral(argument)) return true;
        }
        return false;
    }

    private static boolean literalDp(UExpression expression) {
        expression = unwrap(expression);
        if (!(expression instanceof UQualifiedReferenceExpression qualified)) return false;
        PsiElement resolved = resolvedPsi(qualified.resolve());
        return "androidx.compose.ui.unit.DpKt".equals(owner(resolved))
                && "dp".equals(propertyName(resolved)) && numericLiteral(qualified.getReceiver());
    }

    private static boolean materialColorScheme(UExpression expression) {
        expression = unwrap(expression);
        if (!(expression instanceof UQualifiedReferenceExpression qualified)) return false;
        PsiElement resolved = resolvedPsi(qualified.resolve());
        return "androidx.compose.material3.MaterialTheme".equals(owner(resolved))
                && "colorScheme".equals(propertyName(resolved));
    }

    /** A named factory directly returning a Compose animation spec is itself a motion token. */
    private static boolean namedMotionFactory(UCallExpression node) {
        PsiElement source = node.getSourcePsi();
        if (source == null) return false;
        KtNamedFunction function = PsiTreeUtil.getParentOfType(source, KtNamedFunction.class, false);
        if (function == null || !tokenName(function.getName())) return false;
        KtExpression body = function.getBodyExpression();
        if (body instanceof KtBlockExpression block) {
            List<KtExpression> statements = block.getStatements();
            if (statements.size() != 1 || !(statements.get(0) instanceof KtReturnExpression returned)) {
                return false;
            }
            body = returned.getReturnedExpression();
        }
        while (true) {
            if (body instanceof KtParenthesizedExpression parentheses) body = parentheses.getExpression();
            else if (body instanceof KtDotQualifiedExpression qualified) body = qualified.getSelectorExpression();
            else break;
        }
        return Objects.equals(body, source);
    }

    /** Only immutable property initializers are token definitions; UI methods in the same file are not. */
    private static boolean namedTokenInitializer(UElement node) {
        PsiElement source = node.getSourcePsi();
        if (source == null) return false;
        KtProperty property = PsiTreeUtil.getParentOfType(source, KtProperty.class, false);
        if (property == null || property.isVar() || property.getName() == null
                || property.getInitializer() == null
                || !PsiTreeUtil.isAncestor(property.getInitializer(), source, false)) return false;
        // An initializer that stores UI code or a function is not a literal token definition.
        for (PsiElement ancestor = source; ancestor != null && ancestor != property;
                ancestor = ancestor.getParent()) {
            if (ancestor instanceof KtLambdaExpression || ancestor instanceof KtNamedFunction) return false;
        }
        PsiElement container = property.getParent();
        if (container instanceof KtClassBody && container.getParent() instanceof KtClassOrObject named) {
            return tokenName(named.getName());
        }
        if (container instanceof KtFile file) {
            String name = file.getName();
            int extension = name.lastIndexOf('.');
            return tokenName(extension < 0 ? name : name.substring(0, extension));
        }
        return false;
    }

    private static boolean tokenName(String name) {
        return name != null && (name.endsWith("Spec") || name.endsWith("Palette") || name.endsWith("Policy"));
    }
}
