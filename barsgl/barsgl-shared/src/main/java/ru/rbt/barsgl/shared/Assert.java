package ru.rbt.barsgl.shared;

import java.util.function.Supplier;

/**
 * Вспомогательный класс для проверки предусловий.
 * <p/>
 * Призван заменить код вроде
 * <pre>
 *   if (parameter == null) {
 *     throw new IllegalArgumentException("parameter must not be null");
 *   }
 * </pre>
 * более компактным
 * <pre>
 *   Assert.notNull(parameter, "parameter must not be null");
 * </pre>
 * Название класса и методов выбраны так, чтобы они дополняли друг друга,
 * это идея (и, частично, сами названия) заимствована у Spring Framework
 * (org.springframework.util.Assert).
 *
 * PS: Взято с уважением к создателям из ...
 */
public class Assert {

    private Assert() {}

    /**
     * Удоставеряется, что переданный объект не null,
     * и в противном случае, кидает IllegalArgumentException.
     * <p/>
     * Позвращает проверяемый объект в неизменном виде,
     * для удобства использования в выражениях.
     *
     * @param testee поверяемый объект
     * @return поверяемый объект, если он не null, в неизменном виде
     * @throws IllegalArgumentException если поверяемый объект == null
     */
    public static <T> T notNull(T testee) throws IllegalArgumentException {
        return notNull(testee, "assertion failed: testee must not be null");
    }

    /**
     * Удоставеряется, что переданный объект не null,
     * и в противном случае, кидает IllegalArgumentException.
     * <p/>
     * Позвращает проверяемый объект в неизменном виде,
     * для удобства использования в выражениях.
     *
     * @param testee поверяемый объект
     * @param claim  утверждение, для отладки/документирования, будет помещено в исключение,
     *               если пооверяемый объект == null
     * @return поверяемый объект, если он не null, в неизменном виде
     * @throws IllegalArgumentException если поверяемый объект == null
     */
    public static <T> T notNull(T testee, String claim) throws IllegalArgumentException {
        if (testee == null) {
            throw new IllegalArgumentException(claim);
        }
        return testee;
    }

    /**
     * Удостоверяется, что переданное выражение является истиной,
     * и в противном случае, кидает IllegalArgumentException.
     *
     * @param test проверяемое выражение
     * @throws IllegalArgumentException если вырыжение ложно
     */
    public static void isTrue(boolean test) throws IllegalArgumentException {
        isTrue(test, "assertion failed: test expression must be true");
    }

    /**
     * Аналог Assert.isTrue(boolean)
     * @param test проверяемое выражение
     */
    public static void assertThat(boolean test) throws IllegalArgumentException {
        isTrue(test);
    }

    /**
     * Удостоверяется, что переданное выражение является истиной,
     * и в противном случае, кидает IllegalArgumentException.
     *
     * @param test  проверяемое выражение
     * @param claim утверждение, для отладки/документирования, будет помещено в исключение,
     *              если проверяемое выражение ложно
     * @throws IllegalArgumentException если вырыжение ложно
     */
    public static void isTrue(boolean test, String claim) throws IllegalArgumentException {
        if (!test) {
            throw new IllegalArgumentException(claim);
        }
    }

    public static <X extends Throwable> void isTrue(boolean test, Supplier<? extends X> supplier) throws X {
        if (!test) {
            throw supplier.get();
        }
    }

    /**
     * Удостоверяется, что testee не является пустой строкой
     * @param testee тестируемая строка
     * @param claim сообщение об ошибке, в случае пустой строки
     */
    public static void notEmpty(String testee, String claim) {
        isTrue(null != testee && !testee.isEmpty(), claim);
    }
}
