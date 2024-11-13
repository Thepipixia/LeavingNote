## 深入理解SpringMVC(一)
### 问题提出
SpringMVC是如何接受请求的？是如何匹配到合适的controller的？controller是什么时候被加载的？

### SpringMVC如何接受请求的？

#### Servlet
- Servlet是JavaEE规范的一个接口，当Servlet容器（也称为Web容器，如Tomcat、Jetty、TongWeb等）接受到请求后，会调用Servlet的service方法来处理请求，并返回响应

#### DispatcherServlet（前端处理器）
- DispatcherServlet是SpringMVC的核心，是SpringMVC中唯一一个Servlet，所以Servlet容器的所有请求都会被DispatcherServlet处理

#### 接受请求流程
- 接收到请求后，doDispatch()方法中最重要的就是找到合适的controller来处理请求，在这里controller实际上会被封装为Handler
> Handler和Controller的关系：SpringMVC中所有处理前端请求的方法被称作handler，而controller只是handler的一种实现
- SpringMVC维护了一个HandlerMapping的List，用于存放请求地址和handler的映射关系，通过request中的请求路径即可匹配合适的handler
```java
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    Iterator var2 = this.handlerMappings.iterator();
    while(var2.hasNext()) {
        // 遍历HandlerMapping，通过请求路径找到合适的handler
        HandlerMapping mapping = (HandlerMapping)var2.next();
        HandlerExecutionChain handler = mapping.getHandler(request);
        if (handler != null) {
            return handler;
        }
    }
    return null;
}
```

- 让我们深入了解mapping.getHandler()
```java
protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
    // 根据request获取到请求路径  /user/list  
    String lookupPath = this.initLookupPath(request);
    HandlerMethod handlerMethod = this.lookupHandlerMethod(lookupPath, request);
    return handlerMethod;
}

protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
    // 用于存储匹配的handler
    List<AbstractHandlerMethodMapping<T>.Match> matches = new ArrayList();
    List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(lookupPath);
    if (directPathMatches != null) {
        // 将mapping映射对象封装为Match对象
        this.addMatchingMappings(directPathMatches, matches, request);
    }
    // 在所有匹配的映射中，找到最匹配的，并返回其中的handlerMethod()
    return bestMatch.getHandlerMethod();
}

public List<T> getMappingsByDirectPath(String urlPath) {
    // pathLookup是Spring维护的一个Map的实现类，key是请求路径，value是mappingInfo映射信息对象
    return (List)this.pathLookup.get(urlPath);
}
```
- 可以看到，请求路径可以从request对象中获取，AbstractHandlerMethodMapping维护了一个Map的实现类pathLookup，通过路径进行匹配可以获得Match对象，
，核心pathLookup中的映射信息在后面会讲道
- 获取到handlerMethod后，让我们回到getHandler()方法
```java
public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        // 此时我们已经获得到了匹配的HandlerMethod
        Object handler = this.getHandlerInternal(request);
        HandlerExecutionChain executionChain = this.getHandlerExecutionChain(handler, request);
        return executionChain;
    }
}
```
- 在请求中除了目标方法外，还有众多的拦截器，此时需要将handlerMethod和匹配的拦截器组合成HandlerExecutionChain处理器链
```java
protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
        // 先将handlerMethod封装为拦截器链对象
        HandlerExecutionChain chain = handler instanceof HandlerExecutionChain ? (HandlerExecutionChain)handler : new HandlerExecutionChain(handler);
        Iterator var4 = this.adaptedInterceptors.iterator();
        // 遍历拦截器列表，改拦截器列表由SpringMVC在初始化时已经创建
        while(var4.hasNext()) {
            HandlerInterceptor interceptor = (HandlerInterceptor)var4.next();
            // 判断该请求是否匹配拦截器的拦截规则
            if (mappedInterceptor.matches(request)) {
                // 匹配则加入拦截器链
                chain.addInterceptor(mappedInterceptor.getInterceptor());
            }
        }
        return chain;
    }

```
- 这部分比较简单，就是初始化时会将所有的拦截器放入一个List，遍历所有拦截器，并根据拦截器的拦截规则匹配
- adapter：适配器，是Java的设计模式之一，旨在让不同的转换为相同的接口
- SpringMVC中加入
```java
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
    Iterator var2 = this.handlerAdapters.iterator();
    while(var2.hasNext()) {
         HandlerAdapter adapter = (HandlerAdapter)var2.next();
         if (adapter.supports(handler)) {
             return adapter;
         }
    }
 }
 
 // RequestMappingHandlerAdapter
 protected ModelAndView handleInternal(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
     ModelAndView mav;
     mav = this.invokeHandlerMethod(request, response, handlerMethod);
     return mav;
 }

protected ModelAndView invokeHandlerMethod(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
    // 将HandlerMethod封装为ServletInvocableHandlerMethod对象，使得拥有执行的能力 
    ServletInvocableHandlerMethod invocableMethod = this.createInvocableHandlerMethod(handlerMethod);
    
    // 解析request的参数
    // 解析到的参数会存在成员变量parameters里
     if (this.argumentResolvers != null) {
         invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
     }
     if (this.returnValueHandlers != null) {
         invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
     }
     
     // 反射执行handlerMethod，即正式执行处理器方法
     invocableMethod.invokeAndHandle(webRequest, mavContainer, new Object[0]);
     if (!asyncManager.isConcurrentHandlingStarted()) {
         // 将执行结果封装为ModelAndView对象返回
         ModelAndView var15 = this.getModelAndView(mavContainer, modelFactory, webRequest);
         return var15;
     }
     return (ModelAndView)result;
}

// invocableMethod.invokeAndHandle(webRequest, mavContainer, new Object[0]);最终会走到这里
protected Object doInvoke(Object... args) throws Exception {
    // args参数会在上一级通过成员变量获取
    Method method = this.getBridgedMethod();
    // 反射调用
    return method.invoke(this.getBean(), args);
}
```
- 这里最核心的就是通过handlerMethod获取到参数和Method对象，通过反射调用对应的方法
- InvocableHandlerMethod是将HandlerMethod拥有调用的能力，而ServletInvocableHandlerMethod进一步让他能处理Servlet的请求

#### 初始化流程
- 上文中有很多重要的变量都是初始化的时候创建的，现在来探讨一下它们是如何初始化的
- RequestMappingHandlerMapping实现了InitializingBean接口，会在创建后执行重写的afterPropertiesSet()方法
```java
public void afterPropertiesSet() {
        super.afterPropertiesSet();
    }


public void afterPropertiesSet() {
    this.initHandlerMethods();
}

// AbstractHandlerMethodMapping中
protected void initHandlerMethods() {
    // 获取当前容器中的所有bean
    String[] var1 = this.getCandidateBeanNames();  // this.obtainApplicationContext().getBeanNamesForType(Object.class)
    // 遍历
    int var2 = var1.length;
    for(int var3 = 0; var3 < var2; ++var3) {
        String beanName = var1[var3];
        this.processCandidateBean(beanName);
    }
    this.handlerMethodsInitialized(this.getHandlerMethods());
}

// this.processCandidateBean(beanName);执行到这儿
protected void processCandidateBean(String beanName) {
    Class<?> beanType = null;
    // 根据Bean名称获取类对象
    beanType = this.obtainApplicationContext().getType(beanName);
    if (beanType != null && this.isHandler(beanType)) {
        this.detectHandlerMethods(beanName);
    }
}

// 最核心的方法
protected boolean isHandler(Class<?> beanType) {
    // 判断是否有@Controller注解或@ReqeustMapping注解
    return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) || AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class);
}

protected void detectHandlerMethods(Object handler) {
        Map<Method, T> methods = MethodIntrospector.selectMethods(userType, (method) -> {
            try {
                return this.getMappingForMethod(method, userType);
            } catch (Throwable var4) {
                throw new IllegalStateException("Invalid mapping on handler class [" + userType.getName() + "]: " + method, var4);
            }
        });
}

public static void doWithMethods(Class<?> clazz, MethodCallback mc, @Nullable MethodFilter mf) {
        // 获取所有方法
        Method[] methods = getDeclaredMethods(clazz, false);
        Method[] var4 = methods;
        int var5 = methods.length;
        // 遍历方法
        for(var6 = 0; var6 < var5; ++var6) {
            Method method = var4[var6];
            mc.doWith(method);
        }
}

// method会传到这里
private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
    // 拿到方法上注解对象
    RequestMapping requestMapping = (RequestMapping)AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
    // 根据注解对象中的属性，如：映射路径等，创建对应的RequestMappingInfo对象
    return requestMapping != null ? this.createRequestMappingInfo(requestMapping, condition) : null;
}
```
- 拿到容器中所有的Bean，并通过判断类的对象上有没有对应的注解，如果有，则拿到该类中所有的方法，并拿到方法上的RequestMapping注解对象，通过方法对象和注解对象将信息封装成RequestMappingInfo，
RequestMappingInfo对象中有映射路径、请求方法、请求参数等信息
- 最后就是将RequestMappingInfo注册到pathLookup中
```java
public void register(T mapping, Object handler, Method method) {
    // 创建HandlerMethod对象
    HandlerMethod handlerMethod = AbstractHandlerMethodMapping.this.createHandlerMethod(handler, method);
    // 获取到Mapping的路径，一个方法可以有多个映射路径，所以需要遍历将所有的都放到pathLookup中
    Set<String> directPaths = AbstractHandlerMethodMapping.this.getDirectPaths(mapping);
    Iterator var6 = directPaths.iterator();

    while(var6.hasNext()) {
        String path = (String)var6.next();
        // 放到pathLookup中!!!!!!
        this.pathLookup.add(path, mapping);
    }

    String name = null;
    if (AbstractHandlerMethodMapping.this.getNamingStrategy() != null) {
        // 给handlerMethod生成一个名字，加入到nameLookup中
        name = AbstractHandlerMethodMapping.this.getNamingStrategy().getName(handlerMethod, mapping);
        this.addMappingName(name, handlerMethod);
    }
    this.registry.put(mapping, new MappingRegistration(mapping, handlerMethod, directPaths, name, corsConfig != null));
```

### 总结
- 简单来说，SpringMVC在初始化时，会通过反射匹配的类，并将信息存入成员变量pathLookup变量中。当访问时，通过pathLookup和请求路径匹配，拿到最合适的HandlerMethod，最后通过反射调用对应的方法，返回结果