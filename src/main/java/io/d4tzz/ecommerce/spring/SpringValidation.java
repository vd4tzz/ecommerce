package io.d4tzz.ecommerce.spring;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;

public class SpringValidation {

    public static void validate(Object obj, Validator validator) {
        var bindingResult = new BeanPropertyBindingResult(obj, "obj");
        validator.validate(obj, bindingResult);
        if (bindingResult.hasErrors()) throw new ValidationException(bindingResult);
    }
}
