package com.taxi.easy.ua.ui.cities.Kyiv;

public class KyivCity {
    public static String[]  arrayStreet() {
        return join(Kyiv1.street(),
                Kyiv2.street(),
                Kyiv3.street(),
                Kyiv4.street(),
                Kyiv5.street(),
                Kyiv6.street(),
                Kyiv7.street(),
                Kyiv8.street(),
                Kyiv9.street(),
                Kyiv10.street()
        );
    }

    public static String[] join(String[] a1,
                                String[] a2,
                                String[] a3,
                                String[] a4,
                                String[] a5,
                                String[] a6,
                                String[] a7,
                                String[] a8,
                                String[] a9,
                                String[] a10
    )
    {
        String [] c = new String[a1.length +
                a2.length +
                a3.length +
                a4.length +
                a5.length +
                a6.length +
                a7.length +
                a8.length +
                a9.length +
                a10.length];

        System.arraycopy(a1, 0, c, 0, a1.length);
        System.arraycopy(a2, 0, c, a1.length, a2.length);
        System.arraycopy(a3, 0, c, a1.length
                + a2.length, a3.length);
        System.arraycopy(a4, 0, c, a1.length
                + a2.length
                + a3.length, a4.length);
        System.arraycopy(a5, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length, a5.length);
        System.arraycopy(a6, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length, a6.length);
        System.arraycopy(a7, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length
                + a6.length, a7.length);
        System.arraycopy(a8, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length
                + a6.length
                + a7.length, a8.length);
        System.arraycopy(a9, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length
                + a6.length
                + a7.length
                + a8.length, a9.length);
        System.arraycopy(a10, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length
                + a6.length
                + a7.length
                + a8.length
                + a9.length, a10.length);
        return c;
    }

}
