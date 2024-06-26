package ch.trick17.jtt.testsuitegrader;

public class TestSuiteWhitelists {

    public static final String JUNIT5_DEF = """
            org.junit.jupiter.api.Assertions.*
            org.junit.jupiter.api.Assertions.TimeoutFailureFactory.*
            org.junit.jupiter.api.Assumptions.*
            
            org.junit.jupiter.api.function.Executable.*
            org.junit.jupiter.api.function.ThrowingConsumer.*
            org.junit.jupiter.api.function.ThrowingSupplier.*
            
            org.junit.jupiter.params.provider.Arguments.*
            
            org.opentest4j.AssertionFailedError.*
            """;

    public static final String MOCKITO_DEF = """
            org.mockito.Mockito.mock
            org.mockito.Mockito.mockingDetails
            org.mockito.Mockito.mockConstructionWithAnswer
            org.mockito.Mockito.mockConstruction
            org.mockito.Mockito.spy
            org.mockito.Mockito.when
            org.mockito.Mockito.verify
            org.mockito.Mockito.reset
            org.mockito.Mockito.clearInvocations
            org.mockito.Mockito.verifyNoMoreInteractions
            org.mockito.Mockito.verifyZeroInteractions
            org.mockito.Mockito.verifyNoInteractions
            org.mockito.Mockito.doThrow
            org.mockito.Mockito.doCallRealMethod
            org.mockito.Mockito.doAnswer
            org.mockito.Mockito.doNothing
            org.mockito.Mockito.doReturn
            org.mockito.Mockito.inOrder
            org.mockito.Mockito.ignoreStubs
            org.mockito.Mockito.times
            org.mockito.Mockito.never
            org.mockito.Mockito.atLeastOnce
            org.mockito.Mockito.atLeast
            org.mockito.Mockito.atMostOnce
            org.mockito.Mockito.atMost
            org.mockito.Mockito.calls
            org.mockito.Mockito.only
            org.mockito.Mockito.withSettings
            org.mockito.Mockito.description
            org.mockito.Mockito.lenient
            
            org.mockito.ArgumentCaptor.*
            org.mockito.ArgumentMatcher.*
            org.mockito.ArgumentMatchers.*
            org.mockito.MockedConstruction.*
            org.mockito.MockedConstruction$Context.*
            org.mockito.MockedConstruction$MockInitializer.*
            org.mockito.MockingDetails.*
            org.mockito.MockSettings.*
            org.mockito.InOrder.*
            org.mockito.ScopedMock.*
            
            org.mockito.mock.MockCreationSettings.*
            org.mockito.mock.MockName.*
            org.mockito.mock.SerializableMode.*
            
            org.mockito.stubbing.Answer.*
            org.mockito.stubbing.Answer1.*
            org.mockito.stubbing.Answer2.*
            org.mockito.stubbing.Answer3.*
            org.mockito.stubbing.Answer4.*
            org.mockito.stubbing.Answer5.*
            org.mockito.stubbing.Answer6.*
            org.mockito.stubbing.BaseStubber.*
            org.mockito.stubbing.LenientStubber.*
            org.mockito.stubbing.OngoingStubbing.*
            org.mockito.stubbing.Stubber.*
            org.mockito.stubbing.Stubbing.*
            org.mockito.stubbing.ValidableAnswer.*
            org.mockito.stubbing.VoidAnswer1.*
            org.mockito.stubbing.VoidAnswer2.*
            org.mockito.stubbing.VoidAnswer3.*
            org.mockito.stubbing.VoidAnswer4.*
            org.mockito.stubbing.VoidAnswer5.*
            org.mockito.stubbing.VoidAnswer6.*
            
            org.mockito.listeners.InvocationListener.*
            org.mockito.listeners.MethodInvocationReport.*
            org.mockito.listeners.MockCreationListener.*
            org.mockito.listeners.MockitoListener.*
            org.mockito.listeners.StubbingLookupEvent.*
            org.mockito.listeners.StubbingLookupListener.*
            org.mockito.listeners.VerificationListener.*
            org.mockito.listeners.VerificationStartedEvent.*
            org.mockito.listeners.VerificationStartedListener.*
            
            org.mockito.verification.After.*
            org.mockito.verification.VerificationEvent.*
            org.mockito.verification.VerificationMode.*
            org.mockito.verification.VerificationStrategy.*
            """;

    public static final String SAFE_REFLECTION_DEF = """
            java.lang.Class.isInstance
            java.lang.Class.isAssignableFrom
            java.lang.Class.isInterface
            java.lang.Class.isArray
            java.lang.Class.isPrimitive
            java.lang.Class.isAnnotation
            java.lang.Class.isSynthetic
            java.lang.Class.getName
            java.lang.Class.getModule
            java.lang.Class.getTypeParameters
            java.lang.Class.getSuperclass
            java.lang.Class.getGenericSuperclass
            java.lang.Class.getPackage
            java.lang.Class.getPackageName
            java.lang.Class.getInterfaces
            java.lang.Class.getGenericInterfaces
            java.lang.Class.getComponentType
            java.lang.Class.getModifiers
            java.lang.Class.getEnclosingMethod
            java.lang.Class.getEnclosingConstructor
            java.lang.Class.getDeclaringClass
            java.lang.Class.getEnclosingClass
            java.lang.Class.getSimpleName
            java.lang.Class.getTypeName
            java.lang.Class.getCanonicalName
            java.lang.Class.isAnonymousClass
            java.lang.Class.isLocalClass
            java.lang.Class.isMemberClass
            java.lang.Class.isEnum
            java.lang.Class.isRecord
            # getEnumConstants not supported because it cannot be made re-initializable
            java.lang.Class.getClasses
            java.lang.Class.getFields
            java.lang.Class.getMethods
            java.lang.Class.getConstructors
            java.lang.Class.getField
            java.lang.Class.getMethod
            java.lang.Class.getConstructor
            java.lang.Class.getDeclaredClasses
            java.lang.Class.getDeclaredFields
            java.lang.Class.getRecordComponents
            java.lang.Class.getDeclaredMethods
            java.lang.Class.getDeclaredConstructors
            java.lang.Class.getDeclaredField
            java.lang.Class.getDeclaredMethod
            java.lang.Class.getDeclaredConstructor
            java.lang.Class.cast
            java.lang.Class.asSubclass
            java.lang.Class.getAnnotation
            java.lang.Class.isAnnotationPresent
            java.lang.Class.getAnnotationsByType
            java.lang.Class.getAnnotations
            java.lang.Class.getDeclaredAnnotation
            java.lang.Class.getDeclaredAnnotationsByType
            java.lang.Class.getDeclaredAnnotations
            java.lang.Class.getAnnotatedSuperclass
            java.lang.Class.getAnnotatedInterfaces
            java.lang.Class.getNestHost
            java.lang.Class.isNestmateOf
            java.lang.Class.getNestMembers
            java.lang.Class.descriptorString
            java.lang.Class.componentType
            java.lang.Class.arrayType
            java.lang.Class.isHidden
            java.lang.Class.getPermittedSubclasses
            java.lang.Class.isSealed
            
            java.lang.reflect.Type.*
            java.lang.reflect.Member.*
            java.lang.reflect.AnnotatedElement.*
            java.lang.reflect.Executable.*
            java.lang.reflect.Modifier.*
            
            java.lang.reflect.AccessibleObject.isAccessible
            java.lang.reflect.AccessibleObject.canAccess
            java.lang.reflect.AccessibleObject.getAnnotation
            java.lang.reflect.AccessibleObject.isAnnotationPresent
            java.lang.reflect.AccessibleObject.getAnnotationsByType
            java.lang.reflect.AccessibleObject.getAnnotations
            java.lang.reflect.AccessibleObject.getDeclaredAnnotation
            java.lang.reflect.AccessibleObject.getDeclaredAnnotationsByType
            java.lang.reflect.AccessibleObject.getDeclaredAnnotations
            
            java.lang.reflect.Field.getDeclaringClass
            java.lang.reflect.Field.getName
            java.lang.reflect.Field.getModifiers
            java.lang.reflect.Field.isEnumConstant
            java.lang.reflect.Field.isSynthetic
            java.lang.reflect.Field.getType
            java.lang.reflect.Field.getGenericType
            java.lang.reflect.Field.equals
            java.lang.reflect.Field.hashCode
            java.lang.reflect.Field.toString
            java.lang.reflect.Field.toGenericString
            java.lang.reflect.Field.getAnnotation
            java.lang.reflect.Field.getAnnotationsByType
            java.lang.reflect.Field.getAnnotatedType
            
            java.lang.reflect.Method.getDeclaringClass
            java.lang.reflect.Method.getName
            java.lang.reflect.Method.getModifiers
            java.lang.reflect.Method.getTyeParameters
            java.lang.reflect.Method.getReturnType
            java.lang.reflect.Method.getGenericReturnType
            java.lang.reflect.Method.getParameterTypes
            java.lang.reflect.Method.getParameterCount
            java.lang.reflect.Method.getGenericParameterTypes
            java.lang.reflect.Method.getExceptionTypes
            java.lang.reflect.Method.getGenericExceptionTypes
            java.lang.reflect.Method.equals
            java.lang.reflect.Method.hashCode
            java.lang.reflect.Method.toString
            java.lang.reflect.Method.toGenericString
            java.lang.reflect.Method.isBridge
            java.lang.reflect.Method.isVarArgs
            java.lang.reflect.Method.isSynthetic
            java.lang.reflect.Method.isDefault
            java.lang.reflect.Method.getDefaultValue
            java.lang.reflect.Method.getAnnotation
            java.lang.reflect.Method.getDeclaredAnnotations
            java.lang.reflect.Method.getParameterAnnotations
            java.lang.reflect.Method.getAnnotatedReturnType
            """;
}
