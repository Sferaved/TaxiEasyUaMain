package com.taxi.easy.ua.ui.home.cities.Dnipro;

public class DniproCity {
    public static String[]  arrayStreet() {
        String[] arrayStreet = join(Dnipro1.street(),
                Dnipro2.street()
        );
        return arrayStreet;
    }

    public static String[] join(
            String[] a1,
            String[] a2
    )
    {
        String [] c = new String[a1.length +
                a2.length ];

        System.arraycopy(a1, 0, c, 0, a1.length);
        System.arraycopy(a2, 0, c, a1.length, a2.length);
        return c;
    }

}
