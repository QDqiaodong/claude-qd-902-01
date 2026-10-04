-- 发电日结验收专用种子（H2 兼容；仅汽轮发电模块所需数据）
INSERT INTO turbine_generator
  (unit_no, unit_name, capacity_mw, meter_reading_kwh, grid_meter_reading_kwh,
   meter_source, meter_status, generation_kwh, grid_kwh, auxiliary_rate, running_hours,
   stat_date, status, version, remark) VALUES
  ('TG-01', '1号汽轮发电机组', 25.00, 15860000.00, 13240000.00, 'PREV_DAY', 'NORMAL', 508000.00, 424000.00, 16.54, 24.0,
   DATEADD('DAY', -1, CURRENT_DATE), 'ON_GRID', 0, '凝汽式机组，表码日结'),
  ('TG-02', '2号汽轮发电机组', 25.00, 14520000.00, 12180000.00, 'PREV_DAY', 'NORMAL', 465000.00, 390000.00, 16.13, 24.0,
   DATEADD('DAY', -1, CURRENT_DATE), 'ON_GRID', 0, '抽凝式机组，表码日结'),
  ('TG-03', '3号汽轮发电机组', 12.00,  6980000.00,  5820000.00, 'PREV_DAY', 'NORMAL', 224000.00, 187000.00, 16.52, 24.0,
   DATEADD('DAY', -1, CURRENT_DATE), 'ON_GRID', 0, '小机组，调峰运行');

INSERT INTO generation_daily
  (unit_no, stat_date, base_reading_kwh, closing_reading_kwh, generation_kwh,
   grid_base_reading_kwh, grid_closing_reading_kwh, grid_kwh, auxiliary_rate, running_hours,
   base_type, abnormal_status, operator, remark, create_time) VALUES
  ('TG-03', DATEADD('DAY', -1, CURRENT_DATE), 6756000.00, 6980000.00, 224000.00,
   5633000.00, 5820000.00, 187000.00, 16.52, 24.0,
   'PREV_DAY', 'NORMAL', '日结员甲', '相邻表码增量：期末 6,980,000 − 期初 6,756,000', CURRENT_TIMESTAMP);
