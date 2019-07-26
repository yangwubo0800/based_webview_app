//
//  JPushUtils.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/7/10.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "JPushUtils.h"
#import "JPUSHService.h"

static NSInteger seq = 0;

@implementation JPushUtils

// 处理解析tags字符串成为数组类型
+ (NSSet<NSString *> *)tagListProcess:(NSString *)tag {
    NSArray * tagsList = [tag componentsSeparatedByString:@","];
    NSLog(@"##### tagsList=%@", tagsList);
    NSMutableSet * tags = [[NSMutableSet alloc] init];
    [tags addObjectsFromArray:tagsList];
    //过滤掉无效的tag
    NSSet *newTags = [JPUSHService filterValidTags:tags];
    NSLog(@"##### newTags=%@", newTags);
    return newTags;
}


// 使用逗号分隔来一次设置多个tag
+(void)setJPushTags:(NSString *) tags{
    
    //firstly clean tags and then set tags
    [JPUSHService cleanTags:^(NSInteger iResCode, NSSet *iTags, NSInteger seq) {
        NSLog(@"#########cleanTags iResCode=%ld iTags=%@ seq=%ld", iResCode, iTags, seq);
        //请求需要等前面一个处理完才能继续开始下一个请求
        [JPUSHService setTags:[self tagListProcess:tags] completion:^(NSInteger iResCode, NSSet *iTags, NSInteger seq) {
            NSLog(@"#########===setTags iResCode=%ld iTags=%@ seq=%ld", iResCode, iTags, seq);
        } seq:++seq];
    } seq:++seq];
}



@end
