//
//  CameraController.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/12.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "CameraController.h"
#import "HWScanViewController.h"

static CameraController *instance = nil;


@implementation CameraController


// 单例模式实现
+(CameraController *)shareInstance{
    if (instance == nil) {
        instance = [[CameraController alloc] init];
    }
    
    return instance;
}

//限制方法，类只能初始化一次
//alloc的时候调用
+ (id) allocWithZone:(struct _NSZone *)zone{
    if(instance == nil){
        instance = [super allocWithZone:zone];
    }
    return instance;
}

//拷贝方法
- (id)copyWithZone:(NSZone *)zone{
    return instance;
}




// 行为表
-(void)showActionSheet:(UIViewController *) viewContoller
{
    // 传递视图控制类参数
    self.uiViewController = viewContoller;
    
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"摄像头操作" message:nil preferredStyle:UIAlertControllerStyleActionSheet];
    UIAlertAction *action1 = [UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil];
    
    UIAlertAction *action2 = [UIAlertAction actionWithTitle:@"拍照" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        NSString *mediaType = AVMediaTypeVideo;
        
        AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:mediaType];
        NSLog(@" authStatus is %ld", (long)authStatus);
        if(authStatus == AVAuthorizationStatusRestricted || authStatus == AVAuthorizationStatusDenied){
            //            mAlertView(@"", @"请在'设置'中打开相机权限")
            NSLog(@"请在'设置'中打开相机权限");
            return;
        }
        
        if (![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {
            //            mAlertView(@"", @"照相机不可用")
            NSLog(@"照相机不可用");
            return;
        }
        UIImagePickerController *vc = [[UIImagePickerController alloc] init];
        vc.delegate = self;
        vc.allowsEditing = YES;
        vc.sourceType = UIImagePickerControllerSourceTypeCamera;
        [viewContoller presentViewController:vc animated:YES completion:nil];
    }];
    
    UIAlertAction *action6 = [UIAlertAction actionWithTitle:@"视频" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        NSString *mediaType = AVMediaTypeVideo;
        
        AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:mediaType];
        NSLog(@" authStatus is %ld", (long)authStatus);
        if(authStatus == AVAuthorizationStatusRestricted || authStatus == AVAuthorizationStatusDenied){
            //            mAlertView(@"", @"请在'设置'中打开相机权限")
            NSLog(@"请在'设置'中打开相机权限");
            return;
        }
        
        if (![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {
            //            mAlertView(@"", @"照相机不可用")
            NSLog(@"照相机不可用");
            return;
        }
        UIImagePickerController *vc = [[UIImagePickerController alloc] init];
        vc.delegate = self;
        vc.allowsEditing = YES;
        vc.sourceType = UIImagePickerControllerSourceTypeCamera;
        //设置录制视频参数
        vc.mediaTypes = @[(NSString *)kUTTypeMovie];
        vc.cameraCaptureMode = UIImagePickerControllerCameraCaptureModeVideo;
        vc.videoQuality = UIImagePickerControllerQualityTypeHigh;
        
        [viewContoller presentViewController:vc animated:YES completion:nil];
    }];
    
    UIAlertAction *action7 = [UIAlertAction actionWithTitle:@"扫码" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        NSLog(@"====action7 setupCamera");
        [self scanQRCode:viewContoller];
    }];
    
    
    UIAlertAction *action3 = [UIAlertAction actionWithTitle:@"图片视频选择" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        UIImagePickerController *vc = [[UIImagePickerController alloc] init];
        vc.delegate = self;
        vc.allowsEditing = YES;
        vc.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
        
        //获取相册中的所有类型
        NSArray *availabelMedia = [UIImagePickerController availableMediaTypesForSourceType:vc.sourceType];
        // 视频和图片都获取
        vc.mediaTypes = [[NSArray alloc] initWithArray:availabelMedia];
        
        [viewContoller presentViewController:vc animated:YES completion:nil];
    }];
    
    UIAlertAction *action4 = [UIAlertAction actionWithTitle:@"视频选择" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        UIImagePickerController *vc = [[UIImagePickerController alloc] init];
        vc.delegate = self;
        vc.allowsEditing = YES;
        vc.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
        
        //获取相册中的所有类型
        NSArray *availabelMedia = [UIImagePickerController availableMediaTypesForSourceType:vc.sourceType];
        // 只获取视频
        vc.mediaTypes = [NSArray arrayWithObject:availabelMedia[1]];
        
        [viewContoller presentViewController:vc animated:YES completion:nil];
    }];
    
    UIAlertAction *action5 = [UIAlertAction actionWithTitle:@"图片选择" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        UIImagePickerController *vc = [[UIImagePickerController alloc] init];
        vc.delegate = self;
        vc.allowsEditing = YES;
        vc.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
        
        //获取相册中的所有类型
        NSArray *availabelMedia = [UIImagePickerController availableMediaTypesForSourceType:vc.sourceType];
        // 只获取图片
        vc.mediaTypes = [NSArray arrayWithObject:availabelMedia[0]];
        
        [viewContoller presentViewController:vc animated:YES completion:nil];
    }];
    
    [alert addAction:action1];
    [alert addAction:action2];
    [alert addAction:action6];
    [alert addAction:action7];
    [alert addAction:action3];
    [alert addAction:action4];
    [alert addAction:action5];
    [viewContoller presentViewController:alert animated:YES completion:nil];
}



#pragma mark - 代理方法
/* 拍照或录像成功，都会调用 */
- (void)imagePickerController:(UIImagePickerController *)picker
didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info
{
    NSLog(@" didFinishPickingMediaWithInfo ");
    //从info取出此时摄像头的媒体类型
    NSString *mediaType = [info objectForKey:UIImagePickerControllerMediaType];
    NSLog(@" mediaType is %@", mediaType);
    if ([mediaType isEqualToString:(NSString *)kUTTypeImage]) {//如果是拍照
        //获取拍照的图像
        UIImage *image = [info objectForKey:UIImagePickerControllerOriginalImage];
        //NSString *imageBase64 = [UIImagePNGRepresentation(image) base64EncodedDataWithOptions:NSDataBase64Encoding64CharacterLineLength];
        //NSLog(@"imageBase64 is %@",imageBase64);
        //保存图像到相簿
        UIImageWriteToSavedPhotosAlbum(image, self,
                                       @selector(image:didFinishSavingWithError:contextInfo:), nil);
        
    } else if ([mediaType isEqualToString:(NSString *)kUTTypeMovie]) {//如果是录像
        //获取录像文件路径URL
        NSURL *url = [info objectForKey:UIImagePickerControllerMediaURL];
        NSString *path = url.path;
        //判断能不能保存到相簿
        if (UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(path)) {
            //保存视频到相簿
            UISaveVideoAtPathToSavedPhotosAlbum(path, self,
                                                @selector(video:didFinishSavingWithError:contextInfo:), nil);
        }
        
    }
    //拾取控制器弹回
    [self.uiViewController  dismissViewControllerAnimated:YES completion:nil];
}


/* 取消拍照或录像会调用 */
- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    NSLog(@"取消");
    //拾取控制器弹回
    [self.uiViewController dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - 保存图片或视频完成的回调
- (void)image:(UIImage *)image didFinishSavingWithError:(NSError *)error
  contextInfo:(void *)contextInfo {
    NSLog(@"保存图片完成");
    //    self.showImageView.image = image;
    //    self.showImageView.contentMode = UIViewContentModeScaleToFill;
}

- (void)video:(NSString *)videoPath didFinishSavingWithError:(NSError *)error
  contextInfo:(void *)contextInfo {
    NSLog(@"保存视频完成");
}



// 跳转到扫码界面进行扫码
- (void)scanQRCode:(UIViewController *)webViewController{
    
    UIBarButtonItem *backBtn = [[UIBarButtonItem alloc] init];
    //默认为英文back，此处修改返回标题为中文
    backBtn.title = @"返回";
    webViewController.navigationItem.backBarButtonItem = backBtn;
    
    HWScanViewController *vc = [[HWScanViewController alloc] init];
    [webViewController.navigationController pushViewController:vc animated:YES];
}

// 照相方法
-(void)nativeTakePhoto:(UIViewController *)webViewController{
    self.uiViewController = webViewController;
    NSString *mediaType = AVMediaTypeVideo;
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:mediaType];
    NSLog(@" authStatus is %ld", (long)authStatus);
    if(authStatus == AVAuthorizationStatusRestricted || authStatus == AVAuthorizationStatusDenied){
        //            mAlertView(@"", @"请在'设置'中打开相机权限")
        NSLog(@"请在'设置'中打开相机权限");
        return;
    }
    
    if (![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {
        //            mAlertView(@"", @"照相机不可用")
        NSLog(@"照相机不可用");
        return;
    }
    
    UIImagePickerController *vc = [[UIImagePickerController alloc] init];
    vc.delegate = self;
    vc.allowsEditing = YES;
    vc.sourceType = UIImagePickerControllerSourceTypeCamera;
    [webViewController presentViewController:vc animated:YES completion:nil];
    
}

//录像方法
-(void)nativeRecordVideo:(UIViewController *)webViewController{
    self.uiViewController = webViewController;
    NSString *mediaType = AVMediaTypeVideo;
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:mediaType];
    NSLog(@" authStatus is %ld", (long)authStatus);
    if(authStatus == AVAuthorizationStatusRestricted || authStatus == AVAuthorizationStatusDenied){
        //            mAlertView(@"", @"请在'设置'中打开相机权限")
        NSLog(@"请在'设置'中打开相机权限");
        return;
    }
    
    if (![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {
        //            mAlertView(@"", @"照相机不可用")
        NSLog(@"照相机不可用");
        return;
    }
    
    UIImagePickerController *vc = [[UIImagePickerController alloc] init];
    vc.delegate = self;
    vc.allowsEditing = YES;
    vc.sourceType = UIImagePickerControllerSourceTypeCamera;
    //设置录制视频参数
    vc.mediaTypes = @[(NSString *)kUTTypeMovie];
    vc.cameraCaptureMode = UIImagePickerControllerCameraCaptureModeVideo;
    vc.videoQuality = UIImagePickerControllerQualityTypeHigh;
    
    [webViewController presentViewController:vc animated:YES completion:nil];
}


@end
