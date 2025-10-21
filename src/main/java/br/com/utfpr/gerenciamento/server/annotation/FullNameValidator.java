package br.com.utfpr.gerenciamento.server.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.regex.Pattern;

@Documented
@Constraint(validatedBy = FullNameValidator.FullNameValidatorImpl.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FullNameValidator {
    String message() default "Digite seu nome completo (nome e sobrenome)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class FullNameValidatorImpl implements ConstraintValidator<FullNameValidator, String> {

        private static final String FULL_NAME_REGEX =
                "^(?:[\\p{Lu}&&[\\p{IsLatin}]])(?:(?:')?(?:[\\p{Ll}&&[\\p{IsLatin}]]))+" +
                "(?:\\-(?:[\\p{Lu}&&[\\p{IsLatin}]])(?:(?:')?(?:[\\p{Ll}&&[\\p{IsLatin}]]))+)*" +
                "(?: (?:(?:e|y|de(?: la| las| lo| los)?|do|dos|da|das|del|van(?: der)|von|bin|le) )?" +
                "(?:(?:(?:d'|D'|O'|Mc|Mac|al\\-))?(?:[\\p{Lu}&&[\\p{IsLatin}]])(?:(?:')?(?:[\\p{Ll}&&[\\p{IsLatin}]]))+" +
                "|(?:[\\p{Lu}&&[\\p{IsLatin}]])(?:(?:')?(?:[\\p{Ll}&&[\\p{IsLatin}]]))+" +
                "(?:\\-(?:[\\p{Lu}&&[\\p{IsLatin}]])(?:(?:')?(?:[\\p{Ll}&&[\\p{IsLatin}]]))+)*))+" +
                "(?: (?:Jr\\.|II|III|IV))?$";

        private static final Pattern FULL_NAME_PATTERN = Pattern.compile(FULL_NAME_REGEX);

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }

            String trimmedValue = value.trim();

            if (trimmedValue.isEmpty()) {
                return false;
            }

            if (trimmedValue.length() < 2) {
                return false;
            }

            if (!trimmedValue.contains(" ")) {
                return false;
            }

            String[] parts = trimmedValue.split("\\s+");
            if (parts.length < 2) {
                return false;
            }

            return FULL_NAME_PATTERN.matcher(trimmedValue).matches();
        }
    }
}
