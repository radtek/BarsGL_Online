BEGIN
execute immediate 'ALTER SESSION SET nls_timestamp_format=''YYYY-MM-DD HH24:MI:SS.FF6''';
execute immediate 'ALTER SESSION SET nls_date_format=''YYYY-MM-DD HH24:MI:SS''';
end;
