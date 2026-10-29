import {
  Anchor,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  SimpleGrid,
  Stack,
  Table,
  Text,
  Title,
} from '@mantine/core';
import { formatDate, usePageTitle } from '@orudesk/web-core';
import { Link, useParams } from 'react-router';
import { modeLabel, useDonorDonations } from '../donations';
import { typeLabel, useDonor } from '../donors';

export function DonorPage() {
  const { donorId = '' } = useParams();
  const donor = useDonor(donorId);
  const name = donor.data ? `${donor.data.firstName} ${donor.data.lastName}` : 'Donor';
  usePageTitle(donor.data === null ? 'Donor not found' : name);

  return (
    <Stack>
      <Anchor component={Link} to="/donors" size="sm">
        ← Donors
      </Anchor>
      {donor.isPending && <Loader size="sm" aria-label="Loading" />}
      {donor.isError && <Text role="alert">{donor.error.message}</Text>}
      {donor.data === null && <Title order={1}>Donor not found</Title>}
      {donor.data && (
        <>
          <Group justify="space-between" maw={640}>
            <Group>
              <Title order={1}>{name}</Title>
              <Badge variant="light" tt="none">
                {typeLabel(donor.data.type)}
              </Badge>
            </Group>
            <Group gap="md">
              <Anchor component={Link} to={`/donors/${donor.data.id}/edit`} size="sm">
                Edit
              </Anchor>
              <Anchor component={Link} to={`/activity?subject=${donor.data.id}`} size="sm">
                History
              </Anchor>
            </Group>
          </Group>
          <Card withBorder maw={640}>
            <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md" component="dl" m={0}>
              <Field label="Phone" value={donor.data.phone ?? '—'} />
              <Field label="Email" value={donor.data.email ?? '—'} />
              {/* The API only ever sends the PAN masked (Day 23): the full number never reaches a screen */}
              <Field label="PAN" value={donor.data.panMasked ?? '—'} />
              <Field label="80G receipts" value={donor.data.wantsTaxReceipt ? 'Yes' : 'No'} />
              <Field label="Updates by email" value={donor.data.receivesUpdates ? 'Yes' : 'No'} />
              <Field label="Donor since" value={formatDate(donor.data.registeredOn)} />
            </SimpleGrid>
          </Card>
          <Anchor component={Link} to={`/people/${donor.data.personId}`} size="sm">
            Their entry in the People registry
          </Anchor>
          <Donations donorId={donor.data.id} />
        </>
      )}
    </Stack>
  );
}

/** What the donor gave (Day 60): each donation links to its receipt; cancelled ones stay, struck through. */
function Donations({ donorId }: { donorId: string }) {
  const donations = useDonorDonations(donorId);
  return (
    <Stack maw={640} gap="xs">
      <Group justify="space-between">
        <Title order={2} size="h3">
          Donations
        </Title>
        <Button component={Link} to={`/donors/${donorId}/donations/new`} size="xs">
          Record a donation
        </Button>
      </Group>
      {donations.isPending && <Loader size="sm" aria-label="Loading donations" />}
      {donations.isError && <Text role="alert">{donations.error.message}</Text>}
      {donations.data && donations.data.donations.length === 0 && (
        <Text c="dimmed">No donations yet.</Text>
      )}
      {donations.data && donations.data.donations.length > 0 && (
        <Table aria-label="Donations">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Received</Table.Th>
              <Table.Th>Receipt</Table.Th>
              <Table.Th>Paid by</Table.Th>
              <Table.Th ta="right">Amount</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {donations.data.donations.map((d) => (
              <Table.Tr key={d.id} td={d.cancelledOn ? 'line-through' : undefined}>
                <Table.Td>{formatDate(d.receivedOn)}</Table.Td>
                <Table.Td>
                  <Anchor component={Link} to={`/donations/${d.id}/receipt`}>
                    {d.receiptNumber}
                  </Anchor>
                  {d.cancelledOn && (
                    <Badge color="red" variant="light" ml="xs">
                      Cancelled
                    </Badge>
                  )}
                </Table.Td>
                <Table.Td>{modeLabel(d.mode)}</Table.Td>
                <Table.Td ta="right">{d.amountFormatted}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
          <Table.Tfoot>
            <Table.Tr>
              <Table.Th colSpan={3}>Total</Table.Th>
              <Table.Th ta="right">{donations.data.totalFormatted}</Table.Th>
            </Table.Tr>
          </Table.Tfoot>
        </Table>
      )}
    </Stack>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <Text component="dt" size="sm" c="dimmed">
        {label}
      </Text>
      <Text component="dd" m={0}>
        {value}
      </Text>
    </div>
  );
}
