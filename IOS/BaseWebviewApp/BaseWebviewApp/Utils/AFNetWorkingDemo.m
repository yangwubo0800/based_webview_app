//
//  AFNetWorkingDemo.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/7/25.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "AFNetWorkingDemo.h"

NSString *getUrl = @"http://192.168.137.10:8080/xxxx-area/area/areaInfo/queryAreaWithGradeByConfig";
NSString *postUrl = @"http://192.168.137.10:8080/xxxx-area/area/areaConf/addAreaConfig";
NSString *downloadUrl = @"http://192.168.137.14:8080/FileUploadSampleForApp/upload/launchScreenPicSize.jpg";

@implementation AFNetWorkingDemo


+(void)AFNGet{
    
    AFHTTPSessionManager *manager =[AFHTTPSessionManager manager];
    NSDictionary *dict = @{
                           @"name":@"湖南省",
                           @"grade":@"2"
                           };
    NSLog(@"-------start get .....");
    // parameters 参数字典
    [manager GET:getUrl parameters:dict progress:^(NSProgress * _Nonnull downloadProgress) {
        //进度
        //进度
        NSLog(@"=====downloadProgress is %lld", downloadProgress.completedUnitCount);
    } success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        // task 我们可以通过task拿到响应头
        NSLog(@" success response is %@", task.response);
        
        // responseObject:请求成功返回的响应结果（AFN内部已经把响应体转换为OC对象，通常是字典或数组)
        NSLog(@" success responseObject is %@", responseObject);
        
    } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        // error 错误信息
        NSLog(@" failure response is %@", task.response);
        NSLog(@" failure error is %@", error.description);
    }];
}


+(void)AFNPost{
    
    AFHTTPSessionManager *manager =[AFHTTPSessionManager manager];
    NSDictionary *dict = @{
                           @"name":@"湖南省",
                           };
    // parameters 参数字典
    [manager POST:postUrl parameters:dict progress:^(NSProgress * _Nonnull downloadProgress) {
        //进度
        NSLog(@"=====downloadProgress is %lld", downloadProgress.completedUnitCount);
    } success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        // task 我们可以通过task拿到响应头
        NSLog(@" success response is %@", task.response);
        // responseObject:请求成功返回的响应结果（AFN内部已经把响应体转换为OC对象，通常是字典或数组)
        NSLog(@" success responseObject is %@", responseObject);
        
    } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        // error 错误信息
        NSLog(@" failure response is %@", task.response);
        NSLog(@" failure error is %@", error.description);
    }];
    
}



+(void)downLoad{
    // 1.创建一个管理者
    AFHTTPSessionManager *manager = [AFHTTPSessionManager manager];
    // 2. 创建请求对象
    NSURL *url = [NSURL URLWithString:downloadUrl];
    NSURLRequest *request =[NSURLRequest requestWithURL:url];
    // 3. 下载文件
    NSURLSessionDownloadTask *downloadTask = [manager downloadTaskWithRequest:request progress:^(NSProgress * _Nonnull downloadProgress) {
        // downloadProgress.completedUnitCount 当前下载大小
        // downloadProgress.totalUnitCount 总大小
        NSLog(@"%f", 1.0 * downloadProgress.completedUnitCount / downloadProgress.totalUnitCount);
    } destination:^NSURL * _Nonnull(NSURL * _Nonnull targetPath, NSURLResponse * _Nonnull response) {
        // targetPath  临时存储地址
        NSLog(@"targetPath:%@",targetPath);
        //在模拟器中测试，使用MAC上的目录来放置文件；
        //        NSString *path =[NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) lastObject];
        //        NSString *path = @"/Users/hongboni/Desktop";
        //
        //        NSString *filePath = [path stringByAppendingPathComponent:response.suggestedFilename];
        
        // 在真机中测试，获取Documents目录路径，存放文件；
        NSString *docDir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
        NSLog(@"=====doc dir is %@", docDir);
        NSString *filePath = [docDir stringByAppendingPathComponent:response.suggestedFilename];
        
        NSURL *url = [NSURL fileURLWithPath:filePath];
        NSLog(@"path:%@",filePath);
        // 返回url 我们想要存储的地址
        // response 响应头
        return url;
    } completionHandler:^(NSURLResponse * _Nonnull response, NSURL * _Nullable filePath, NSError * _Nullable error) {
        // 下载完成之后调用
        // response 响应头
        // filePath 下载存储地址
        NSLog(@"filePath:%@", filePath);
        //NSLog(@"completionHandler response:%@", response);
    }];
    
    // 需要手动开启
    [downloadTask resume];
}



+(void)upLoad{
    
    AFHTTPSessionManager *manager = [AFHTTPSessionManager manager];
    //windows 部署后台，因为课件管理中使用的是inpect模块来记录上传文件的
    NSString *url = @"http://192.168.137.14:8080/xxxx-CourseManage/coursemanage/cmFile/addCmFile";
    // MAC 部署后台上传,返回response 需要兼容修改
    //NSString *url = @"http://192.168.137.10:8080/FileUploadSampleForApp/UploadFile";
    
    
    [manager POST:url parameters:nil constructingBodyWithBlock:^(id<AFMultipartFormData>  _Nonnull formData) {
        // formData 将要上传的数据
        // 直接传URL
        NSURL *url =[NSURL fileURLWithPath:@"/Users/hongboni/Desktop/image_welcome.jpg"];
        
        NSString *mimeType = [self connectSync:@"/Users/hongboni/Desktop/image_welcome.jpg"];
        // 方法一
        [formData appendPartWithFileURL:url name:@"file" fileName:@"image_welcome.jpg" mimeType:mimeType  error:nil];
        // 方法二
        /**
         这个方法会自动截取url最后一块的文件名作为上传到服务器的文件名
         也会自动获取mimeType，如果没有办法获取mimeType 就使用@"application/octet-stream" 表示任意的二进制数据 ，当我们不在意文件类型的时候 也可以用这个。
         */
        //[formData appendPartWithFileURL:url name:@"file" error:nil];
        
    } progress:^(NSProgress * _Nonnull uploadProgress) {
        // 上传进度
        NSLog(@"upload progress is %lld", uploadProgress.completedUnitCount / uploadProgress.totalUnitCount);
        
    } success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        // TODO: 只要有response返回就会走这里，即使是返回失败，所以需要针对返回结果进行解析处理。
        // 上传成功
        NSLog(@"上传成功");
        NSLog(@"responseObject:%@", responseObject);
    } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        // 没有response 返回的时候走这里
        // 上传失败
        NSLog(@"上传失败");
    }];
    
}


// 通过发送请求获取mimeType
+(NSString *)connectSync:(NSString *)path
{
    //1.确定请求路径
    NSURL *url = [NSURL fileURLWithPath:path];
    
    //NSURL *url =[NSURL fileURLWithPath:@"/Users/hongboni/Desktop/real.txt"];
    
    //2.创建可变的请求对象
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    NSHTTPURLResponse *res = nil;
    [NSURLConnection sendSynchronousRequest:request returningResponse:&res error:nil];
    NSLog(@"%@",res.MIMEType);
    return res.MIMEType;
}


@end
