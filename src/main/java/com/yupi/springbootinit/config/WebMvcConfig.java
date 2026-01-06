package com.yupi.springbootinit.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:./uploads/");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToNumberWhenNotBlankFactory());
    }

    /**
     * 允许空串转换成 null，再交给 Spring 的逻辑去校验
     */
    public static class StringToNumberWhenNotBlankFactory
            implements ConverterFactory<String, Number> {

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
            return source -> {
                if (!org.springframework.util.StringUtils.hasText(source)) {
                    return null;              // ① 空串 → null
                }
                if (Long.class == targetType) {
                    return (T) Long.valueOf(source);
                }
                if (Integer.class == targetType) {
                    return (T) Integer.valueOf(source);
                }
                throw new IllegalStateException("Unsupported Number type: " + targetType);
            };
        }
    }
}
