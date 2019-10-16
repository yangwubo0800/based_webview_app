//
//  GeneralUtils.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/10/10.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "GeneralUtils.h"

@implementation GeneralUtils

//1. 获取缓存文件的大小
+( NSString* )getCacheSize{
    NSString *cachePath = [NSSearchPathForDirectoriesInDomains (NSCachesDirectory , NSUserDomainMask , YES) firstObject];
    return [ self folderSizeAtPath :cachePath];
}


//由于缓存文件存在沙箱中，我们可以通过NSFileManager API来实现对缓存文件大小的计算。
// 遍历文件夹获得文件夹大小，返回多少 M
+ ( NSString* ) folderSizeAtPath:( NSString *) folderPath{
    
    NSFileManager * manager = [NSFileManager defaultManager];
    if (![manager fileExistsAtPath :folderPath]) return 0 ;
    NSEnumerator *childFilesEnumerator = [[manager subpathsAtPath :folderPath] objectEnumerator];
    NSString * fileName;
    long long folderSize = 0 ;
    while ((fileName = [childFilesEnumerator nextObject]) != nil ){
        //获取文件全路径
        NSString * fileAbsolutePath = [folderPath stringByAppendingPathComponent :fileName];
        folderSize += [ self fileSizeAtPath :fileAbsolutePath];
        NSLog(@"fileName is %@", fileName);
    }
    
    NSString *formatSize = [self getFormatSize:folderSize];
    return formatSize;
    
}



// 计算 单个文件的大小
+ ( long long ) fileSizeAtPath:( NSString *) filePath{
    NSFileManager * manager = [NSFileManager defaultManager];
    if ([manager fileExistsAtPath :filePath]){
        return [[manager attributesOfItemAtPath :filePath error : nil] fileSize];
    }
    return 0;
}


// 格式化缓存大小，单位为 KB MB GB
+(NSString *)getFormatSize:(long long) size{
    NSString *formatSize;
    float kiloByte = size / 1024;
    if (kiloByte < 1) {
        return @"0KB";
    }
    
    float megaByte = kiloByte / 1024;
    if (megaByte < 1) {
        formatSize = [NSString stringWithFormat:@"%.2fKB", kiloByte];
        return formatSize;
    }
    
    float gigaByte = megaByte / 1024;
    if (gigaByte < 1) {
        formatSize = [NSString stringWithFormat:@"%.2fMB", megaByte];
        return formatSize;
    }
    
    float teraBytes = gigaByte / 1024;
    if (teraBytes < 1) {
        formatSize = [NSString stringWithFormat:@"%.2fGB", gigaByte];
        return formatSize;
    }
    
    formatSize = [NSString stringWithFormat:@"%.2fTB", teraBytes];
    return formatSize;

}



//2. 清除缓存
+ (void)clearCache
{
    NSString * cachePath = [NSSearchPathForDirectoriesInDomains (NSCachesDirectory , NSUserDomainMask , YES ) firstObject];
    NSArray * files = [[NSFileManager defaultManager ] subpathsAtPath :cachePath];
    //NSLog ( @"cachpath = %@" , cachePath);
    for ( NSString * p in files) {
        
        NSError * error = nil ;
        //获取文件全路径
        NSString * fileAbsolutePath = [cachePath stringByAppendingPathComponent :p];
        
        if ([[NSFileManager defaultManager ] fileExistsAtPath :fileAbsolutePath]) {
            [[NSFileManager defaultManager ] removeItemAtPath :fileAbsolutePath error :&error];
        }
    }
    
    //读取缓存大小
//    float cacheSize = [self readCacheSize] *1024;
//    self.cacheSize.text = [NSString stringWithFormat:@"%.2fKB",cacheSize];
    
}

@end
