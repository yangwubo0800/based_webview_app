//
//  StringUtils.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/27.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "StringUtils.h"

@implementation StringUtils

+ (BOOL)isBlankString:(NSString *)string{
    
    if (string == nil) {
        
        return YES;
        
    }
    
    if (string == NULL) {
        
        return YES;
        
    }
    
    if ([string isKindOfClass:[NSNull class]]) {
        
        return YES;
        
    }
    
    if ([[string stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]] length]==0) {
        
        return YES;
        
    }
    
    return NO;
    
}


+ (NSString *)UIUtilsFomateJsonWithDictionaryReverse:(NSDictionary *)dic {
    
    NSArray *keys = [dic allKeys];
    NSString *string = [NSString string];

    for (NSString *key in keys) {
        NSString *value = [dic objectForKey:key];
        value = [NSString stringWithFormat:@"\"%@\"",value];
        NSString *newkey = [NSString stringWithFormat:@"\"%@\"",key];
        
        if (!string.length) {
            string = [NSString stringWithFormat:@"%@:%@}",newkey,value];
            
        }else {
            string = [NSString stringWithFormat:@"%@:%@,%@",newkey,value,string];
        }
    }

    string = [NSString stringWithFormat:@"{%@",string];
    
    return string;
    
}


+ (NSString *)UIUtilsFomateJsonWithDictionary:(NSDictionary *)dic {
    
    NSArray *keys = [dic allKeys];
    NSString *string = [NSString string];
    
    for (NSString *key in keys) {
        NSString *value = [dic objectForKey:key];
        value = [NSString stringWithFormat:@"\"%@\"",value];
        NSString *newkey = [NSString stringWithFormat:@"\"%@\"",key];
        
        if (!string.length) {
            string = [NSString stringWithFormat:@"{%@:%@",newkey,value];
            
        }else {
            string = [NSString stringWithFormat:@"%@,%@:%@",string,newkey,value];
        }
    }
    string = [NSString stringWithFormat:@"%@}",string];
    
    return string;
}


@end
