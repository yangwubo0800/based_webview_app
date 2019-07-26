//
//  CameraController.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/12.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import <Photos/Photos.h>

NS_ASSUME_NONNULL_BEGIN

@interface CameraController : NSObject <UIImagePickerControllerDelegate, UINavigationControllerDelegate, AVCaptureMetadataOutputObjectsDelegate>

// 相册选择控制器
@property(nonatomic, strong) UIImagePickerController *pickerController;

// 传递过来的视图控制器
@property(nonatomic, strong) UIViewController *uiViewController;

// 扫码需要用到的设备
@property (nonatomic, strong) AVCaptureSession *session;
@property (nonatomic, strong) AVCaptureDevice *device;
@property (nonatomic, strong) AVCaptureDeviceInput *input;
@property (nonatomic, strong) AVCaptureMetadataOutput *outPut;
@property (nonatomic, strong) AVCaptureVideoPreviewLayer *previewLayer;
/**
 展示输出流的视图——即照相机镜头下的内容
 */
@property (nonatomic, strong) UIView *preview;


// 单例模式
+(CameraController *)shareInstance;

// 提供给外部使用的 行为方法alert
-(void)showActionSheet:(UIViewController *) viewContoller;

// 扫码功能
- (void)scanQRCode:(UIViewController *)webViewController;

// 拍照
-(void)nativeTakePhoto:(UIViewController *)webViewController;

// 录像
-(void)nativeRecordVideo:(UIViewController *)webViewController;

@end

NS_ASSUME_NONNULL_END
