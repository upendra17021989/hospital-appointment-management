-- Creates snapshot line-items table for staff-entered consultation receipts

create table if not exists consultation_receipt_line_items (
    id uuid primary key,
    receipt_id uuid not null,
    sr_no integer not null,
    particulars varchar(250) not null,
    amount numeric(10,2) not null,

    constraint fk_consultation_receipt_line_items_receipt
        foreign key (receipt_id) references consultation_receipts(id)
        on delete cascade
);

create index if not exists idx_consultation_receipt_line_items_receipt_id
    on consultation_receipt_line_items(receipt_id);


