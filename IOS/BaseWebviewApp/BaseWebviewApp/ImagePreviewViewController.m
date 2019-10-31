//
//  ImagePreviewViewController.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/10/28.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "ImagePreviewViewController.h"
#import "YHPhotoBrowser.h"


//单例模式
static ImagePreviewViewController *instance = nil;

@interface ImagePreviewViewController ()

@end

@implementation ImagePreviewViewController


// 单例模式实现
+(ImagePreviewViewController *)shareInstance{
    if (instance == nil) {
        instance = [[ImagePreviewViewController alloc] init];
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


- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}



- (void)show:(NSString *)imageUrl{
    
//    NSArray *srchightArray = @[@"http://ww2.sinaimg.cn/bmiddle/904c2a35jw1emu3ec7kf8j20c10epjsn.jpg"];
    NSLog(@"=====show image url is %@", imageUrl);
    NSArray *srchightArray = @[imageUrl];
    
    /*
     @"http://ww2.sinaimg.cn/bmiddle/67307b53jw1epqq3bmwr6j20c80axmy5.jpg",
     @"http://ww2.sinaimg.cn/bmiddle/9ecab84ejw1emgd5nd6eaj20c80c8q4a.jpg"
     @"http://ww2.sinaimg.cn/bmiddle/67307b53jw1epqq3bmwr6j20c80axmy5.jpg",
     @"http://ww2.sinaimg.cn/bmiddle/9ecab84ejw1emgd5nd6eaj20c80c8q4a.jpg",
     @"http://ww2.sinaimg.cn/bmiddle/9ecab84ejw1emgd5nd6eaj20c80c8q4a.jpg",
     @"http://ww2.sinaimg.cn/bmiddle/642beb18gw1ep3629gfm0g206o050b2a.gif",
     @"http://ww1.sinaimg.cn/bmiddle/9be2329dgw1etlyb1yu49j20c82p6qc1.jpg"
     @"http://ww2.sinaimg.cn/bmiddle/9ecab84ejw1emgd5nd6eaj20c80c8q4a.jpg",
     @"http://ww2.sinaimg.cn/bmiddle/67307b53jw1epqq3bmwr6j20c80axmy5.jpg",
     
     @"http://ww2.sinaimg.cn/bmiddle/642beb18gw1ep3629gfm0g206o050b2a.gif",
     @"http://ww1.sinaimg.cn/bmiddle/9be2329dgw1etlyb1yu49j20c82p6qc1.jpg"
     */
    
    
    YHPhotoBrowser *photoView=[[YHPhotoBrowser alloc]init];
    photoView.sourceView=self.view;         // 图片所在的父容器
    photoView.urlImgArr=srchightArray;           //网络链接图片的数组
    photoView.sourceRect=self.imgView.frame;   // 图片的frame
    photoView.indexTag=0;                      //初始化进去显示的图片下标
    [photoView show];
    
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
