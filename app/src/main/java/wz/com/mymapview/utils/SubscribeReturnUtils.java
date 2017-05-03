package wz.com.mymapview.utils;

/**
 * 点击单车,预约单车显示提示
 * Created by cifz on 2017/2/23.
 * e_mail wangzhen1798@gmail.com
 */

public class SubscribeReturnUtils {
    private static String[] clickTips = new String[]{"小主，上车，带你兜风","放马过来吧","我在遥望，月亮之上","everybody，come here","来吧！上来呀，哥"};

    /**
     * 点击返回提示
     * @return
     */
    public static String ClickReturnTips(){
        return clickTips[returIndex()];
    }

    /**
     * 返回随机数
     * @return
     */
    private static int returIndex(){
        int index = 0+(int)(Math.random()*4);
        return index;
    }

}
