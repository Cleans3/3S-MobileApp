-- Dev demo catalog (profile `dev` only). ISBNs are made up.
-- Copy number n (1..24) has tag_id '0000000000' || two-hex n || '04E0' (e.g. 00000000000104E0) and barcode 'SHELF-000n'.

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-000000000001', '9780990000013', 'Truyện Kiều', array['Nguyễn Du'], array['Thơ', 'Văn học Việt Nam'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'SHELF-0001', '00000000000104E0', 'A01', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'SHELF-0002', '00000000000204E0', 'A01', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-000000000002', '9780990000020', 'Dế Mèn phiêu lưu ký', array['Tô Hoài'], array['Thiếu nhi', 'Văn học Việt Nam'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'SHELF-0003', '00000000000304E0', 'A02', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'SHELF-0004', '00000000000404E0', 'A02', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000002', 'SHELF-0005', '00000000000504E0', 'A02', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-000000000003', '9780990000037', 'Số đỏ', array['Vũ Trọng Phụng'], array['Tiểu thuyết', 'Văn học Việt Nam'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003', 'SHELF-0006', '00000000000604E0', 'A03', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003', 'SHELF-0007', '00000000000704E0', 'A03', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-000000000004', '9780990000044', 'Tắt đèn', array['Ngô Tất Tố'], array['Tiểu thuyết', 'Văn học Việt Nam'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000004', 'SHELF-0008', '00000000000804E0', 'A04', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-000000000005', '9780990000051', 'Lão Hạc', array['Nam Cao'], array['Truyện ngắn', 'Văn học Việt Nam'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000005', 'SHELF-0009', '00000000000904E0', 'A05', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-000000000006', '9780990000068', 'Chí Phèo', array['Nam Cao'], array['Truyện ngắn', 'Văn học Việt Nam'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-000000000006', 'SHELF-0010', '00000000000A04E0', 'A06', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-00000000000b', '10000000-0000-0000-0000-000000000006', 'SHELF-0011', '00000000000B04E0', 'A06', 'WITHDRAWN');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-000000000007', '9780990000075', 'Clean Code', array['Robert C. Martin'], array['Software engineering', 'Programming'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-00000000000c', '10000000-0000-0000-0000-000000000007', 'SHELF-0012', '00000000000C04E0', 'A07', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-00000000000d', '10000000-0000-0000-0000-000000000007', 'SHELF-0013', '00000000000D04E0', 'A07', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-00000000000e', '10000000-0000-0000-0000-000000000007', 'SHELF-0014', '00000000000E04E0', 'A07', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-000000000008', '9780990000082', 'Introduction to Algorithms', array['Thomas H. Cormen', 'Charles E. Leiserson', 'Ronald L. Rivest', 'Clifford Stein'], array['Algorithms', 'Computer science'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-00000000000f', '10000000-0000-0000-0000-000000000008', 'SHELF-0015', '00000000000F04E0', 'A08', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000008', 'SHELF-0016', '00000000001004E0', 'A08', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-000000000009', '9780990000099', 'The Pragmatic Programmer', array['Andrew Hunt', 'David Thomas'], array['Software engineering', 'Programming'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000009', 'SHELF-0017', '00000000001104E0', 'A09', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-00000000000a', '9780990000105', 'Designing Data-Intensive Applications', array['Martin Kleppmann'], array['Databases', 'Distributed systems'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-00000000000a', 'SHELF-0018', '00000000001204E0', 'A10', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-00000000000a', 'SHELF-0019', '00000000001304E0', 'A10', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-00000000000b', '9780990000112', 'Java Card Technology for Smart Cards', array['Zhiqun Chen'], array['Smart cards', 'Java'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-00000000000b', 'SHELF-0020', '00000000001404E0', 'A11', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-00000000000c', '9780990000129', 'Applied Cryptography', array['Bruce Schneier'], array['Cryptography', 'Security'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-00000000000c', 'SHELF-0021', '00000000001504E0', 'A12', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-00000000000c', 'SHELF-0022', '00000000001604E0', 'A12', 'LENDABLE');

insert into title (id, isbn, title, authors, subjects, cover_url) values ('10000000-0000-0000-0000-00000000000d', '9780990000136', 'Android Programming: The Big Nerd Ranch Guide', array['Bill Phillips', 'Chris Stewart', 'Kristin Marsicano'], array['Android', 'Mobile development'], null);
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-00000000000d', 'SHELF-0023', '00000000001704E0', 'A13', 'LENDABLE');
insert into copy (id, title_id, barcode, tag_id, shelf, status) values ('20000000-0000-0000-0000-000000000018', '10000000-0000-0000-0000-00000000000d', 'SHELF-0024', '00000000001804E0', 'A13', 'LENDABLE');
