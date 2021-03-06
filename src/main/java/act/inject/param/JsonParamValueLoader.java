package act.inject.param;

import act.app.ActionContext;
import act.app.App;
import act.app.data.StringValueResolverManager;
import act.inject.DependencyInjector;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.inject.BeanSpec;

import javax.inject.Provider;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

class JsonParamValueLoader implements ParamValueLoader {

    private static final String KEY_JSON_DTO = JsonDTO.CTX_KEY;
    private static final Provider NULL_PROVIDER = new Provider() {
        @Override
        public Object get() {
            return null;
        }
    };

    private ParamValueLoader fallBack;
    private BeanSpec spec;
    private Provider defValProvider;
    private boolean isPathVariable;

    JsonParamValueLoader(ParamValueLoader fallBack, BeanSpec spec, DependencyInjector<?> injector) {
        this.fallBack = $.notNull(fallBack);
        this.spec = $.notNull(spec);
        this.defValProvider = findDefValProvider(spec, injector);
        ActionContext ctx = ActionContext.current();
        if (null != ctx) {
            Set<String> pathVariables = ctx.attribute(ActionContext.ATTR_PATH_VARS);
            isPathVariable = pathVariables.contains(spec.name());
        }
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (isPathVariable) {
            return fallBack.load(bean, context, noDefaultValue);
        }
        JsonDTO dto = context.attribute(KEY_JSON_DTO);
        if (null == dto) {
            return this.fallBack.load(bean, context, noDefaultValue);
        } else {
            context.attribute(KEY_JSON_DTO, dto);
            Object o = dto.get(spec.name());
            return null != o ? o : defValProvider.get();
        }
    }

    private static Provider findDefValProvider(BeanSpec beanSpec, DependencyInjector<?> injector) {
        final Class c = beanSpec.rawType();
        final StringValueResolverManager resolver = App.instance().resolverManager();
        if (c.isPrimitive()) {
            return new Provider() {
                @Override
                public Object get() {
                    return resolver.resolve(null, c);
                }
            };
        } else if (Collection.class.isAssignableFrom(c) || Map.class.isAssignableFrom(c)) {
            return injector.getProvider(c);
        } else if (c.isArray()) {
            return new Provider() {
                @Override
                public Object get() {
                    return Array.newInstance(c.getComponentType(), 0);
                }
            };
        } else {
            return NULL_PROVIDER;
        }
    }

}
