package mtn.lgdx.uavclient;


public class UAVCmd {
	
	public static final int ACCELERATOR 	= 0X1;	//油门
	public static final int RIGHT_LEFT 		= 0X2;	//左右
	public static final int ROTATE 			= 0X3;	//旋转
	public static final int FORWARD_BACK 	= 0X4;	//前后
	public static final int START 			= 0X5;	//启动命令
	public static final int START_DELAY 	= 0X6;	//启动延时命令
	
	public static final int MEDIAN 		= 0X3FF; 	//滑动条在中间位置发送的数据

	public static int FormatCmd(int type , int data ){
		//发送两个字节的命令,最高的四位表示命令类型,低12位是数据
		return ((type<<12 & 0xffff) + data);
	}
}
