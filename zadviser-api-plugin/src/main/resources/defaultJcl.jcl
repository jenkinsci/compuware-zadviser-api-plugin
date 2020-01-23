//ZADVISER JOB (\'EXTRACT\',4WOODWARD),                  
//             \'CPWR\',                                 
//             CLASS=A,                                  
//             MSGCLASS=X,                               
//             REGION=0M,                                
//             NOTIFY=&SYSUID                         
//*
//* DO NOT TOUCH THE DATE FIELD(S) BELOW AS THEY WILL BE OVERRIDDEN WHEN JCL IS PROCESSED
//*                                                                                                
//*  DUMP ZADVISER SMF RECORDS (NON-LOGSTREAM)
//*                                                       
//STEP1    EXEC PGM=IFASMFDP                                 
//SMFIN    DD DISP=SHR,DSN=MIS.SMF.CW01.DAILY                
//OUTDD1   DD  DISP=(,PASS),DSN=&&SMFOUT,                    
//             UNIT=VIO,SPACE=(CYL,(3000,3000),RLSE),       
//             DCB=(RECFM=VBS,LRECL=32760,BLKSIZE=4096)   
//SYSPRINT DD  SYSOUT=*                                   
//SYSIN    DD  *                                            
 INDD(SMFIN,OPTIONS(DUMP))                                   
DATE(XXXXX,XXXXX)
 OUTDD(OUTDD1,TYPE(241))                                   