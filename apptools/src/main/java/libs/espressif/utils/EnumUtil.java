package libs.espressif.utils;

public class EnumUtil {
    /**
     * Get the enum instance with the name
     *
     * @param enumClass the class of the enum
     * @param nameStr   the name string of the enum
     * @param <E>       Class Enum
     * @return target name enum
     */
    public static <E extends Enum<E>> E getEnumWithName(Class<E> enumClass, String nameStr) {
        if (enumClass.isEnum()) {
            E[] enums = enumClass.getEnumConstants();
            for (E e : enums) {
                if (e.name().equals(nameStr)) {
                    return e;
                }
            }
        }

        return null;
    }
}
