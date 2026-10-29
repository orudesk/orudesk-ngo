import { Alert, Anchor, Button, Group, Select, Stack, Text, TextInput, Title } from '@mantine/core';
import { usePageTitle, ValidationError } from '@orudesk/web-core';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { PAYMENT_MODES, today, useRecordDonation, type PaymentMode } from '../donations';
import { useDonor } from '../donors';

/** Record money a donor gave (Day 60). The receipt opens as soon as it's saved, ready to print. */
export function RecordDonationPage() {
  const { donorId = '' } = useParams();
  const donor = useDonor(donorId);
  const record = useRecordDonation();
  const navigate = useNavigate();
  const [mode, setMode] = useState<PaymentMode>('UPI');
  const name = donor.data ? `${donor.data.firstName} ${donor.data.lastName}` : '';
  usePageTitle('Record a donation');
  const fieldErrors = record.error instanceof ValidationError ? record.error.fields : {};
  const reference = PAYMENT_MODES.find((m) => m.value === mode)?.reference ?? null;

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const text = (field: string) => String(form.get(field) ?? '').trim();
    record.mutate(
      {
        donorId,
        // Sent as text, as typed: the API keeps it exact. "1,500" is a typing habit, not part of the number.
        amount: text('amount').replaceAll(',', ''),
        receivedOn: text('receivedOn'),
        mode,
        reference: reference ? text('reference') || null : null,
        purpose: text('purpose') || null,
      },
      { onSuccess: (id) => void navigate(`/donations/${id}/receipt`, { replace: true }) },
    );
  };

  return (
    <Stack maw={520}>
      <Anchor component={Link} to={`/donors/${donorId}`} size="sm">
        ← {name || 'Donor'}
      </Anchor>
      <Title order={1}>Record a donation</Title>
      {name && <Text>From {name}</Text>}
      {record.isError && !(record.error instanceof ValidationError) && (
        <Alert color="red" role="alert" title="Not saved">
          {record.error.message}
        </Alert>
      )}
      <form onSubmit={submit} noValidate>
        <Stack>
          <TextInput
            name="amount"
            label="Amount (₹)"
            inputMode="decimal"
            placeholder="1500.00"
            withAsterisk
            maw={220}
            error={fieldErrors.amount}
          />
          <TextInput
            name="receivedOn"
            label="Received on"
            type="date"
            defaultValue={today()}
            withAsterisk
            maw={200}
            error={fieldErrors.receivedOn}
          />
          <Select
            label="Paid by"
            data={PAYMENT_MODES.map(({ value, label }) => ({ value, label }))}
            value={mode}
            onChange={(value) => value && setMode(value as PaymentMode)}
            allowDeselect={false}
            maw={320}
            description={
              mode === 'CASH' ? 'Cash of ₹2,00,000 or more can’t be accepted' : undefined
            }
          />
          {reference && (
            <TextInput
              name="reference"
              label={reference}
              withAsterisk
              error={fieldErrors.reference}
            />
          )}
          <TextInput
            name="purpose"
            label="Towards"
            placeholder="General"
            error={fieldErrors.purpose}
          />
          <Group>
            <Button type="submit" loading={record.isPending}>
              Save and show receipt
            </Button>
            <Button component={Link} to={`/donors/${donorId}`} variant="default">
              Cancel
            </Button>
          </Group>
        </Stack>
      </form>
    </Stack>
  );
}
