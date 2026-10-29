import {
  Alert,
  Anchor,
  Badge,
  Button,
  Card,
  Divider,
  Group,
  Loader,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { EditConflictAlert, EditConflictError, formatDate, usePageTitle } from '@orudesk/web-core';
import { IconPrinter } from '@tabler/icons-react';
import { type FormEvent } from 'react';
import { Link, useParams } from 'react-router';
import { modeLabel, useCancelDonation, useDonation, useReceipt } from '../donations';
import './receipt.css';

/**
 * A donation's receipt (Day 60), laid out to print on A4 with the browser's Print: only the receipt goes on paper.
 * Below it, for the office: cancel the donation (a bounced cheque), with a reason.
 */
export function ReceiptPage() {
  const { donationId = '' } = useParams();
  const receipt = useReceipt(donationId);
  const donation = useDonation(donationId);
  const cancel = useCancelDonation(donationId);
  const r = receipt.data;
  usePageTitle(r ? `Receipt ${r.receiptNumber}` : 'Receipt');

  const submitCancel = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!donation.data) return;
    const reason = String(new FormData(event.currentTarget).get('reason') ?? '').trim();
    cancel.mutate({ version: donation.data.version, reason });
  };

  return (
    <Stack maw={720}>
      {r && (
        <Group justify="space-between" className="no-print">
          <Anchor component={Link} to={`/donors/${donation.data?.donorId ?? ''}`} size="sm">
            ← {r.donorName}
          </Anchor>
          <Button
            leftSection={<IconPrinter size={16} />}
            variant="default"
            onClick={() => window.print()}
          >
            Print
          </Button>
        </Group>
      )}
      {receipt.isPending && <Loader size="sm" aria-label="Loading" />}
      {receipt.isError && <Text role="alert">{receipt.error.message}</Text>}
      {r === null && <Title order={1}>Receipt not found</Title>}
      {r && (
        <Card withBorder padding="xl" className="receipt">
          <Stack gap="xs">
            <Group justify="space-between" align="flex-start">
              <div>
                <Title order={1} size="h3">
                  {r.organisationName}
                </Title>
                {r.organisationAddress && <Text size="sm">{r.organisationAddress}</Text>}
                {r.organisationPan && <Text size="sm">PAN {r.organisationPan}</Text>}
                {r.registration80G && <Text size="sm">80G registration {r.registration80G}</Text>}
              </div>
              <Stack gap={0} align="flex-end">
                <Text fw={700}>Donation receipt</Text>
                <Text>No. {r.receiptNumber}</Text>
                <Text size="sm">Issued {formatDate(r.issuedOn)}</Text>
              </Stack>
            </Group>
            {r.cancelledOn && (
              <Badge color="red" size="lg" variant="outline">
                Cancelled on {formatDate(r.cancelledOn)}: {r.cancelReason}
              </Badge>
            )}
            <Divider my="sm" />
            <Table withRowBorders={false} verticalSpacing={4}>
              <Table.Tbody>
                <Row label="Received from" value={r.donorName} />
                {r.donorPan && <Row label="Donor's PAN" value={r.donorPan} />}
                <Row label="Amount" value={r.amountFormatted} strong />
                <Row label="In words" value={r.amountInWords} />
                <Row label="Received on" value={formatDate(r.receivedOn)} />
                <Row
                  label="Paid by"
                  value={modeLabel(r.mode) + (r.reference ? ` (${r.reference})` : '')}
                />
                {r.purpose && <Row label="Towards" value={r.purpose} />}
              </Table.Tbody>
            </Table>
            <Divider my="sm" />
            <Text size="sm">
              {r.taxDeductible
                ? 'This donation qualifies for deduction under section 80G of the Income Tax Act, 1961.'
                : `Not for a tax deduction: ${r.notDeductibleBecause}.`}
            </Text>
          </Stack>
        </Card>
      )}
      {r && !r.cancelledOn && donation.data && (
        <Card withBorder className="no-print">
          <form onSubmit={submitCancel} noValidate>
            <Stack gap="xs">
              <Text fw={500}>Cancel this donation</Text>
              <Text size="sm" c="dimmed">
                For a cheque that bounced or an entry made twice. The receipt number stays, marked
                cancelled, and the donation leaves the totals.
              </Text>
              {cancel.error instanceof EditConflictError ? (
                <EditConflictAlert
                  onReload={() => void donation.refetch().then(() => cancel.reset())}
                  reloading={donation.isFetching}
                />
              ) : (
                cancel.isError && (
                  <Alert color="red" role="alert">
                    {cancel.error.message}
                  </Alert>
                )
              )}
              <Group align="flex-end">
                <TextInput
                  name="reason"
                  label="Reason"
                  required
                  maxLength={200}
                  style={{ flex: 1 }}
                />
                <Button type="submit" color="red" variant="light" loading={cancel.isPending}>
                  Cancel donation
                </Button>
              </Group>
            </Stack>
          </form>
        </Card>
      )}
    </Stack>
  );
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <Table.Tr>
      <Table.Th w={160} fw={400} c="dimmed">
        {label}
      </Table.Th>
      <Table.Td fw={strong ? 700 : undefined}>{value}</Table.Td>
    </Table.Tr>
  );
}
