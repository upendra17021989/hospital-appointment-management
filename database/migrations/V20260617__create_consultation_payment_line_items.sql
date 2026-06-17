-- Snapshot line items entered during consultation payment creation
-- for later receipt/PDF printing.

create table if not exists consultation_payment_line_items (
    id uuid primary key,
    payment_id uuid not null,
    sr_no integer not null,
    particulars varchar(250) not null,
    amount numeric(10,2) not null,

    constraint fk_consultation_payment_line_items_payment
        foreign key (payment_id) references consultation_payments(id)
        on delete cascade
);

create index if not exists idx_consultation_payment_line_items_payment_id
    on consultation_payment_line_items(payment_id);

